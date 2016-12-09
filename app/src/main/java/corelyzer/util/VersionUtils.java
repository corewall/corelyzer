package corelyzer.util;

public class VersionUtils {
	// strip qualifying text from a version string e.g. "-beta" from "1.2.3-beta"
	public static String stripVersionQualifier(String version) {
		int[] delims = { '-', '_', ' ' };
		for (int i = 0; i < delims.length; i++) {
			int idx = version.indexOf(delims[i]);
			if (idx != -1) {
				version = version.substring(0, idx);
				break;
			}
		}

		return version;
	}

	// return true if version1 >= version2 
	public static boolean isLatestVersion(String version1, String version2) throws NumberFormatException {
		String[] v1 = stripVersionQualifier(version1).split("\\.");
		String[] v2 = stripVersionQualifier(version2).split("\\.");

		boolean latest = false;
		boolean diffFound = false;
		int index = 0;
		while (index < v1.length && index < v2.length) {
			try {
				int comp = Integer.valueOf(v1[index]).compareTo(Integer.valueOf(v2[index]));
				if (comp != 0) {
					latest = (comp > 0); // comp > 0 implies v1 > v2
					diffFound = true;
					break;
				}
			} catch (NumberFormatException nfe) {
				throw nfe;
			}
			index++;
		}
		
		// if equivalent up to this point, longer version is greater
		if (!diffFound)
			latest = v1.length >= v2.length;
			
		return latest;
	}
	
	// todo: automated testing
//	System.out.println(isLatestVersion("2", "2"));
//	System.out.println(isLatestVersion("2", "1"));
//	System.out.println(isLatestVersion("1.0.0", "1.0.0"));
//	System.out.println(isLatestVersion("1.0.1", "1.0.0"));
//	System.out.println(isLatestVersion("1.0.1", "1.0"));
//	System.out.println(isLatestVersion("1.0.1", "1.0.0.0"));
//	System.out.println(isLatestVersion("1.2.3.0", "1.2.3"));
//	System.out.println(isLatestVersion("1.2.3.0", "1.2.4"));

}
