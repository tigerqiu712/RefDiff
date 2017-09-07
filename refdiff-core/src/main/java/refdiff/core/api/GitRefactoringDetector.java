package refdiff.core.api;

import org.eclipse.jgit.lib.Repository;

/**
 * Detect refactorings in a git commit.
 * 
 */
public interface GitRefactoringDetector {

	/**
	 * Detect refactorings performed in the specified commit. 
	 * 
	 * @param repository A git repository (from JGit library).
	 * @param beforecommitId The SHA key that identifies the commit.
	 * @param aftercommitId The SHA key that identifies the commit.
	 * @param handler A handler object that is responsible to process the detected refactorings. 
	 */
	void detectAtCommit(Repository repository, String beforecommitId,String aftercommitId, RefactoringHandler handler);

	/**
	 * @return An ID that represents the current configuration for the algorithm in use.
	 */
	String getConfigId();
}
