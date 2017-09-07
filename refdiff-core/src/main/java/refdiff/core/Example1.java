package refdiff.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;

import refdiff.core.RefDiff;
import refdiff.core.api.GitService;
import refdiff.core.rm2.model.refactoring.SDRefactoring;
import refdiff.core.util.GitServiceImpl;

public class Example1 {
	public static void main(String[] args) throws Exception {
		RefDiff refDiff = new RefDiff();
		GitService gitService = new GitServiceImpl();

		// try (Repository repository =
		// gitService.cloneIfNotExists("/Users/admin/Documents/git/jersey",
		// "https://github.com/jersey/jersey.git")) {

		try {
			Repository repository = gitService.cloneIfNotExists(
					"/Users/admin/Documents/git/junit4",
					"https://github.com/junit-team/junit4.git");

			System.out.println(repository.getFullBranch());
			System.out.println(repository.getConfig().toString());

			List<SDRefactoring> refactorings = refDiff.detectAtCommit(
					repository, "a5acc2defe0134d179cf15424202b5403eef74d4","24dfdd762e753c83babca3768d4b508cf5c6a86c");
			System.out.println(refactorings.toString());
			for (SDRefactoring r : refactorings) {
				System.out.println("getRefactoringType"+r.getRefactoringType().name());
				System.out.println("getAbbreviation"+r.getRefactoringType().getAbbreviation());
				System.out.println("getDisplayName"+ r.getRefactoringType().getDisplayName());
				
				System.out.println( "Key#"+r.getEntityBefore().key());
			    System.out.println( "Key# "+r.getEntityAfter().key());

				System.out.println( "sourceCode#"+r.getEntityBefore().sourceCode());
			    System.out.println( "sourceCode# "+r.getEntityAfter().sourceCode());
			    
				System.out.println( "fullName#"+r.getEntityBefore().fullName());
			    System.out.println( "fullName# "+r.getEntityAfter().fullName());
			    
			    
			}
			Iterator<Map.Entry<String, Map<Integer,Integer>>> entries =refDiff.LineMethods.entrySet().iterator() ;
	        while (entries.hasNext()) {  
	        	  
	            Map.Entry<String, Map<Integer,Integer>> entry = entries.next();  
	          
	            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
	             Map<Integer,Integer> entryLines =entry.getValue();
	            
	             
	            int lineNumber = entryLines.entrySet().iterator().next().getKey();
	    		System.out.println("Modify lines from   "+lineNumber); 
	    	    lineNumber = entryLines.entrySet().iterator().next().getValue() ;
	     		System.out.println("Modify lines to   "+lineNumber); 
	  
	        }  
			 refactorings = refDiff.detectAtCommit(
					repository,"376c2fc3f269eaba580c75cd1689ca2ba16ad202", "852762b48959776ba0c6da72bac8b403005d6fb4");
			System.out.println(refactorings.toString());
			for (SDRefactoring r : refactorings) {
				System.out.println(r.getRefactoringType().name());
				System.out.println(r.getRefactoringType().getAbbreviation());
				System.out.println("type"+ r.getRefactoringType().getDisplayName());
				System.out.println( "Key#"+r.getEntityBefore().key());
			    System.out.println( "Key# "+r.getEntityAfter().key());

				System.out.println( "sourceCode#"+r.getEntityBefore().sourceCode());
			    System.out.println( "sourceCode# "+r.getEntityAfter().sourceCode());
			    
				System.out.println( "fullName#"+r.getEntityBefore().fullName());
			    System.out.println( "fullName# "+r.getEntityAfter().fullName());


			}
		} catch (Exception e) {
			System.out.println("testEx, catch exception"+e.getMessage());
		}
	}

}
