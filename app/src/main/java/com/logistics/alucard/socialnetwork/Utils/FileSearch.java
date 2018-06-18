package com.logistics.alucard.socialnetwork.Utils;

import java.io.File;
import java.util.ArrayList;

public class FileSearch {

    /**
     * Search a directory and return a list of all the **directories** inside the directory
     * @param directory
     * @return
     */
    public static ArrayList<String> getDirectoryPaths(String directory) {
        ArrayList<String> pathArray = new ArrayList<>();
        File file = new File(directory);
        File[] listfiles = file.listFiles();
        for(int i = 0; i < listfiles.length; i++) {
            if(listfiles[i].isDirectory()) {
                pathArray.add(listfiles[i].getAbsolutePath());
            }
        }
        return pathArray;
    }

    /**
     * Search a directory and return a list of all the **files** inside the directory
     * @param files
     * @return
     */
    public static ArrayList<String> getFilesPaths(String files) {
        ArrayList<String> pathArray = new ArrayList<>();
        File file = new File(files);
        File[] listfiles = file.listFiles();
        for(int i = 0; i < listfiles.length; i++) {
            if(listfiles[i].isFile()) {
                pathArray.add(listfiles[i].getAbsolutePath());
            }
        }
        return pathArray;

    }
}
