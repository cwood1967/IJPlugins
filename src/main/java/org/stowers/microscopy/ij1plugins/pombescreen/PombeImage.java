package org.stowers.microscopy.ij1plugins.pombescreen;

public class PombeImage {
/***
 * Gene ID
 * Entry Clone
 * Gene Name
 * Gene Synonyms/Obsolete
 * ORF Length (Unspliced)
 * Sequence Results
 * Expression levels
 * Localization
 * Additional Information
 */


    String geneId;
    String entryClone;
    String geneName;
    String geneAlias;
    String orfLength;
    String sequenceResults;
    String expressionLevels;
    String localization;
    String info;

    String filename;
    String fnum;
    String path;

    public PombeImage(String geneId, String entryClone, String geneAlias,
                      String orfLength, String sequenceResults, String expressionLevels,
                      String localization, String info, String path) {

        this.geneId = geneId;
        this.entryClone = entryClone;
        this.geneAlias = geneAlias;
        this.orfLength = orfLength;
        this.sequenceResults = sequenceResults;
        this.expressionLevels = expressionLevels;
        this.localization = localization;
        this.info = info;
        this.path = path;


    }

    private void fillFileInfo() {

        String[] s =  geneId.split("/");
        fnum = s[0];
        filename = fnum + s[1];

    }

}
