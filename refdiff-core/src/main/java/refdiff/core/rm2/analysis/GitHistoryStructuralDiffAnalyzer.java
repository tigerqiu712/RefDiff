package refdiff.core.rm2.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import refdiff.core.util.DiffMatchPatch;

public class GitHistoryStructuralDiffAnalyzer {

	Logger logger = LoggerFactory
			.getLogger(GitHistoryStructuralDiffAnalyzer.class);
	private final RefDiffConfig config;

	public GitHistoryStructuralDiffAnalyzer() {
		this(new RefDiffConfigImpl());
	}

	public GitHistoryStructuralDiffAnalyzer(RefDiffConfig config) {
		this.config = config;
	}

	public Map<String, Map<Integer, Integer>> beforelLineMethods = new HashMap<String, Map<Integer, Integer>>();

	public Map<String, Map<Integer, Integer>> afterLineMethods = new HashMap<String, Map<Integer, Integer>>();

	private LinkedHashMap<String, String> beforeMethodsBody = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> afterMethodsBody = new LinkedHashMap<String, String>();
	public LinkedHashMap<String, String> MethodsBody = new LinkedHashMap<String, String>();

	public void detectAtCommit(Repository repository, String beforeCommitId,
			String afterCommitId, StructuralDiffHandler handler) {
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		// RevWalk walk = new RevWalk(repository);
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository
					.resolve(beforeCommitId));

			this.detectRefactorings(gitService, repository, handler,
					projectFolder, commit, afterCommitId);

		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error",
					beforeCommitId), e);
			handler.handleException(beforeCommitId, e);
		}
	}

	protected void detectRefactorings(GitService gitService,
			Repository repository, final StructuralDiffHandler handler,
			File projectFolder, RevCommit currentCommit, String afterCommit)
			throws Exception {
		String commitId = currentCommit.getId().getName();
		List<String> filesBefore = new ArrayList<String>();
		List<String> filesCurrent = new ArrayList<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, currentCommit, afterCommit,
				filesBefore, filesCurrent, renamedFilesHint, false);
		// If no java files changed, there is no refactoring. Also, if there are
		// only ADD's or only REMOVE's there is no refactoring
		System.out.println(filesBefore);
		System.out.println(filesCurrent);
		SDModelBuilder builder = new SDModelBuilder(config);
		if (filesBefore.isEmpty() || filesCurrent.isEmpty()) {
			return;
		}
		// Checkout and build model for current commit
		File folderAfter = new File(projectFolder.getParentFile(), "v1/"
				+ projectFolder.getName() + "-" + commitId.substring(0, 7));
		if (folderAfter.exists()) {
			logger.info(String
					.format("Analyzing code after (%s) ...", commitId));
			builder.analyzeAfter(folderAfter, filesCurrent);

		} else {
			gitService.checkout(repository, commitId);
			logger.info(String
					.format("Analyzing code after (%s) ...", commitId));
			builder.analyzeAfter(projectFolder, filesCurrent);
		}
		beforelLineMethods.clear();
		beforelLineMethods.putAll(builder.lineMethods);
		beforeMethodsBody.clear();
		beforeMethodsBody.putAll(builder.methodsBody);
		String parentCommit = afterCommit;
		File folderBefore = new File(projectFolder.getParentFile(), "v0/"
				+ projectFolder.getName() + "-" + commitId.substring(0, 7));
		if (folderBefore.exists()) {
			logger.info(String.format("Analyzing code before (%s) ...",
					parentCommit));
			builder.analyzeBefore(folderBefore, filesBefore);
		} else {
			// Checkout and build model for parent commit
			gitService.checkout(repository, parentCommit);
			logger.info(String.format("Analyzing code before (%s) ...",
					parentCommit));
			builder.analyzeBefore(projectFolder, filesBefore);
		}
		// }
		beforelLineMethods.putAll(builder.lineMethods);
		afterMethodsBody.clear();
		afterMethodsBody.putAll(builder.methodsBody);

		final SDModel model = builder.buildModel();
		handler.handle(currentCommit, model);
		compareMapByEntrySet(beforeMethodsBody, afterMethodsBody);
	}

	public void compareMapByEntrySet(Map<String, String> before,
			Map<String, String> after) {

		DiffMatchPatch diff = new DiffMatchPatch();
		if (before.size() != after.size()) {

		}

		String beforebody;
		String afterbody;
		LinkedList linkedlist = new LinkedList();
		// handle modify methods
		for (Map.Entry<String, String> entry : before.entrySet()) {
			if (after.containsKey(entry.getKey())) {
				beforebody = entry.getValue();

				afterbody = after.get(entry.getKey());

				linkedlist.clear();
				linkedlist = diff.diff_main(beforebody, afterbody, true);

				if (linkedlist.size() > 1) {
					MethodsBody.put(entry.getKey(), entry.getValue());

				}

			}

		}
		// handle added methods
		for (Map.Entry<String, String> entry : after.entrySet()) {
			if (!before.containsKey(entry.getKey())) {

				MethodsBody.put(entry.getKey(), entry.getValue());

			}

		}

	}
}
