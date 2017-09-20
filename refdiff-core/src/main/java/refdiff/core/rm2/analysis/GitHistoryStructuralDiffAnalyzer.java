package refdiff.core.rm2.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import refdiff.core.api.GitService;
import refdiff.core.rm2.model.SDModel;
import refdiff.core.util.GitServiceImpl;

public class GitHistoryStructuralDiffAnalyzer {

	Logger logger = LoggerFactory.getLogger(GitHistoryStructuralDiffAnalyzer.class);
	private final RefDiffConfig config;
	
	public GitHistoryStructuralDiffAnalyzer() {
        this(new RefDiffConfigImpl());
    }
	
	public GitHistoryStructuralDiffAnalyzer(RefDiffConfig config) {
        this.config = config;
    }

	public Map<String, Map<Integer,Integer>> beforelLineMethods= new HashMap<String, Map<Integer,Integer>>();

	public Map<String, Map<Integer,Integer>> afterLineMethods= new HashMap<String, Map<Integer,Integer>>();


	public void detectAtCommit(Repository repository, String beforeCommitId,String afterCommitId, StructuralDiffHandler handler) {
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		//RevWalk walk = new RevWalk(repository);
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(beforeCommitId));
			
			 this.detectRefactorings(gitService, repository, handler, projectFolder, commit,afterCommitId);
			
		} catch (Exception e) {
		    logger.warn(String.format("Ignored revision %s due to error", beforeCommitId), e);
		    handler.handleException(beforeCommitId, e);
        }
	}
	
	protected void detectRefactorings(GitService gitService, Repository repository, final StructuralDiffHandler handler, File projectFolder, RevCommit currentCommit,String afterCommit) throws Exception {
	    String commitId = currentCommit.getId().getName();
		List<String> filesBefore = new ArrayList<String>();
		List<String> filesCurrent = new ArrayList<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, currentCommit,afterCommit, filesBefore, filesCurrent, renamedFilesHint, false);
		// If no java files changed, there is no refactoring. Also, if there are
		// only ADD's or only REMOVE's there is no refactoring
		System.out.println(filesBefore);
		System.out.println(filesCurrent);
		SDModelBuilder builderbefore = new SDModelBuilder(config);
		if (filesBefore.isEmpty() || filesCurrent.isEmpty()) {
		    return;
		}
			// Checkout and build model for current commit
	    File folderAfter = new File(projectFolder.getParentFile(), "v1/" + projectFolder.getName() + "-" + commitId.substring(0, 7));
	    if (folderAfter.exists()) {
	        logger.info(String.format("Analyzing code after (%s) ...", commitId));
	        builderbefore.analyzeAfter(folderAfter, filesCurrent);
	        
	    } else {
	        gitService.checkout(repository, commitId);
	        logger.info(String.format("Analyzing code after (%s) ...", commitId));
	        builderbefore.analyzeAfter(projectFolder, filesCurrent);
	    }
	    beforelLineMethods.clear();
	    beforelLineMethods.putAll(builderbefore.lineMethods);
	    SDModelBuilder builderafter = new SDModelBuilder(config);
	    String parentCommit =  afterCommit;
		File folderBefore = new File(projectFolder.getParentFile(), "v0/" + projectFolder.getName() + "-" + commitId.substring(0, 7));
		if (folderBefore.exists()) {
		    logger.info(String.format("Analyzing code before (%s) ...", parentCommit));
		    builderafter.analyzeBefore(folderBefore, filesBefore);
		} else {
		    // Checkout and build model for parent commit
		    gitService.checkout(repository, parentCommit);
		    logger.info(String.format("Analyzing code before (%s) ...", parentCommit));
		    builderafter.analyzeBefore(projectFolder, filesBefore);
		}
//		}
		afterLineMethods.clear();
		afterLineMethods.putAll(builderafter.lineMethods);
		Iterator<Map.Entry<String, Map<Integer,Integer>>> entries =afterLineMethods.entrySet().iterator() ;
        while (entries.hasNext()) {  
        	  
            Map.Entry<String, Map<Integer,Integer>> entry = entries.next();  
          
            System.out.println("afterLineMethods Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
             Map<Integer,Integer> entryLines =entry.getValue();
            
             
            int lineNumber = entryLines.entrySet().iterator().next().getKey();
    		System.out.println("afterLineMethods Modify lines from   "+lineNumber); 
    	    lineNumber = entryLines.entrySet().iterator().next().getValue() ;
     		System.out.println("afterLineMethods Modify lines to   "+lineNumber); 
  
        }  	
		final SDModel model = builderafter.buildModel();
		handler.handle(currentCommit, model);
	}

}
