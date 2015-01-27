package org.dynamide.restreplay;

import org.dynamide.util.Tools;

import java.util.*;

/**
 * User: laramie
 * $LastChangedRevision:  $
 * $LastChangedDate:  $
 * STATUS.MISSING means the Right tree did not have a node that the left tree had. (RIGHT_MISSING)
 * STATUS.ADDED means the Right tree had a node that the left tree did not have.(RIGHT_ADDED)
 * RestReplay comparse expected versus payload by walking the nodes (of JSON or XML) of the two trees of payloads.
 * The Left tree is the expected, from the test.  The Right tree is the payload from the service.
 */
public class TreeWalkResults extends ArrayList<TreeWalkResults.TreeWalkEntry> {
      public String toString(String LEAD){
          StringBuffer res = new StringBuffer();
          for (TreeWalkResults.TreeWalkEntry entry: this) {
              res.append(entry.toString(LEAD));
          }
          return  res.toString();
      }

    /** This cllass has two public Lists: you can construct your own to set the acceptable and unacceptable STATUS codes.
     *   They are defaulted to ADDED being acceptable. */
    public static class MatchSpec {
        public static final TreeWalkEntry.STATUS[]  defaultAcceptableStatiArray = {TreeWalkEntry.STATUS.INFO,
                                                                                         TreeWalkEntry.STATUS.MATCHED,
                                                                                         TreeWalkEntry.STATUS.ADDED};

        public static final TreeWalkEntry.STATUS[] defaultErrorStatiArray =           {TreeWalkEntry.STATUS.MISSING,
                                                                                          TreeWalkEntry.STATUS.NESTED_ERROR,
                                                                                          TreeWalkEntry.STATUS.DIFFERENT,
                                                                                          TreeWalkEntry.STATUS.ERROR};
        public List<TreeWalkEntry.STATUS> errorStati;

        public static MatchSpec createDefault(){
            MatchSpec result = new MatchSpec();
            result.errorStati = Arrays.asList(defaultErrorStatiArray);
            return result;
        }
        public static MatchSpec create(TreeWalkEntry.STATUS[] statiArray){
            MatchSpec result = new MatchSpec();
            result.errorStati = Arrays.asList(statiArray);
            return result;
        }
        public void removeErrorFromSpec(TreeWalkEntry.STATUS status){
            ArrayList arrayList = new ArrayList(errorStati);
            arrayList.remove(status);
            errorStati = arrayList;
        }
        public String toString(){
            StringBuffer buff = new StringBuffer("{");
            int i = 0;
            for (TreeWalkEntry.STATUS status : errorStati){
                 if (i>0) buff.append(",");
                String foo = status.toString();
                buff.append(foo);
                i++;
            }
            buff.append("}");
            return buff.toString();
        }

    }

    public static class TreeWalkEntry {
        public String lpath = "";
        public String rpath = "";
        public String ltextTrimmed = "";
        public String rtextTrimmed = "";
        public String expected = "";
        public String actual = "";
        public String message = "";
        public String errmessage = "";
        public TreeWalkResults nested;
        /* STATUS.MISSING means the Right tree did not have a node that the left tree had.
         * STATUS.ADDED means the Right tree had a node that the left tree did not have.
         */
        public static enum STATUS {INFO, MATCHED, MISSING, ADDED, ERROR, DIFFERENT, NESTED_ERROR};
        public STATUS status;
        public String toString(){
            return toString("\r\n");
        }
        public String toString(String LEAD){
            String INDENT = "    ";
            return
                 LEAD + "{"
                 +status.name()
                 +(Tools.notEmpty(lpath) ? ", L.path:"+lpath : "")
                 +(Tools.notEmpty(rpath) ? ", R.path:"+rpath : "")
                 +(Tools.notEmpty(message) ? ", message:"+message : "")
                 +(Tools.notEmpty(errmessage) ? ", errmessage:"+errmessage : "")
                 +", status:"+status
                 +((status != STATUS.MATCHED) && Tools.notEmpty(ltextTrimmed) ? ","+LEAD+"    L.trimmed:"+ltextTrimmed : "")
                 +((status != STATUS.MATCHED) && Tools.notEmpty(rtextTrimmed) ? ","+LEAD+"    R.trimmed:"+rtextTrimmed : "")
                 +((status != STATUS.MATCHED) && Tools.notEmpty(expected) ? LEAD+"EXPECTED:"+LEAD+"------------------"+LEAD+expected.trim()+LEAD+"------------------" : "")
                 +((status != STATUS.MATCHED) && Tools.notEmpty(actual) ? LEAD+"ACTUAL:"+LEAD+"------------------"+LEAD+actual.trim()+LEAD+"------------------"+LEAD : "")
                 +((status != STATUS.MATCHED) && (nested != null) ? LEAD+"NESTED:"+LEAD+"------------------"+LEAD+nested.toString(LEAD+INDENT)+LEAD+"------------------"+LEAD : "")
                 +"}";
        }
    }

    public boolean hasDocErrors(){
        for (TreeWalkEntry entry : this){
            if (entry.status == TreeWalkEntry.STATUS.ERROR){
                return true;
            }
        }
        return false;
    }

    public String getErrorMessages(){
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (TreeWalkEntry entry : this){
            if ( Tools.notEmpty(entry.errmessage)){
                if (first) {
                    buf.append(",errors:");
                } else {
                    buf.append(',');
                }
                buf.append('\''+entry.errmessage+"\'");
                first = false;
            }
        }
        return buf.toString();
    }



    public boolean isStrictMatch(){
        for (TreeWalkEntry entry : this){
            if (entry.status == TreeWalkEntry.STATUS.ERROR){
                return false;
            }
            if ( !(   entry.status == TreeWalkEntry.STATUS.MATCHED
                   || entry.status == TreeWalkEntry.STATUS.INFO)){
                return false;
            }
        }
        return true;
    }
    public int getMismatchCount(){
        int c = 0;
        for (TreeWalkEntry entry : this){
            if ( entry.status == TreeWalkEntry.STATUS.ERROR
                || entry.status != TreeWalkEntry.STATUS.MATCHED
                || entry.status != TreeWalkEntry.STATUS.INFO){
                c++;
            }
        }
        return c;
    }
    /** For our purposes, trees match if they have the same element tree structure - no checking is done for text node changes. */
    public boolean treesMatch(){
        for (TreeWalkEntry entry : this){
            if (entry.status == TreeWalkEntry.STATUS.ERROR
                || entry.status == TreeWalkEntry.STATUS.MISSING
                || entry.status == TreeWalkEntry.STATUS.ADDED){
                return false;
            }
        }
        return true;
    }

    public boolean treesMatch(MatchSpec matchSpec) {
        for (TreeWalkEntry entry : this) {
            if (matchSpec.errorStati.contains(entry.status)) {
                return false;
            }
        }
        return true;
    }

    public int countFor(TreeWalkEntry.STATUS status){
        int count = 0;
        for (TreeWalkEntry entry : this){
            if (entry.status.equals(status)){
                count++;
            }
        }
        return count;
    }

    public String miniSummary(){
        //MATCHED, INFO, MISSING, ADDED, DIFFERENT};
        StringBuffer buf = new StringBuffer();
        buf.append("{");
        boolean nextline = false;
        for (TreeWalkEntry.STATUS st : TreeWalkEntry.STATUS.values()){
            if (nextline) buf.append(',');
            buf.append(st.name()+':'+countFor(st));
            nextline = true;
        }
        buf.append(getErrorMessages());
        buf.append("}");
        return buf.toString();
    }

    public String fullSummary(){
        StringBuffer buf = new StringBuffer();
        buf.append("STATUS: ").append(miniSummary());
        for (TreeWalkResults.TreeWalkEntry entry : this){
            buf.append(entry.toString()).append("\r\n");
        }
        String errMessages = getErrorMessages();
        if (Tools.notBlank(errMessages)) {
            buf.append("\r\n=====errs=====================\r\n");
            buf.append(errMessages);
        }
        return buf.toString();
    }


    public String leftID;
    public String rightID;

    public static Map<TreeWalkEntry.STATUS,Range> createDOMSet(String ma,
                                                               String mi,
                                                               String ad,
                                                               String de,
                                                               String te,
                                                               String ne){
            Map<TreeWalkEntry.STATUS,Range> rangeMap = new HashMap<TreeWalkEntry.STATUS, Range>();
            rangeMap.put(TreeWalkEntry.STATUS.MATCHED, new Range(ma));
            rangeMap.put(TreeWalkEntry.STATUS.MISSING, new Range(mi));
            rangeMap.put(TreeWalkEntry.STATUS.ADDED, new Range(ad));
            rangeMap.put(TreeWalkEntry.STATUS.ERROR, new Range(de));
            rangeMap.put(TreeWalkEntry.STATUS.DIFFERENT, new Range(te));
            rangeMap.put(TreeWalkEntry.STATUS.NESTED_ERROR, new Range(ne));
            return rangeMap;
    }

}