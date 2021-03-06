/*
 * BookmarkQuery.java
 *
 * Created on 6.11.2006, 22:24
 * Copyright (c) 2005-2008, Eugene Stahov (evgs), http://bombus-im.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * You can also redistribute and/or modify this program under the
 * terms of the Psi License, specified in the accompanied COPYING
 * file, as published by the Psi Project; either dated January 1st,
 * 2005, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package xmpp.extensions.muc;

import Client.StaticData;
import com.alsutton.jabber.JabberBlockListener;
import com.alsutton.jabber.JabberDataBlock;
import com.alsutton.jabber.datablocks.Iq;
import java.util.Enumeration;
import java.util.Vector;
import Client.Config;
import com.alsutton.jabber.datablocks.Presence;

import util.StringLoader;

//#ifdef PRIVACY
//# import PrivacyLists.QuickPrivacy;
//#endif
/**
 *
 * @author Evg_S
 */
public class BookmarkQuery implements JabberBlockListener {

    public final static boolean SAVE = true;
    public final static boolean LOAD = false;
    private StaticData sd = StaticData.getInstance();
    private Config cf = Config.getInstance();
    boolean queryType;

    /** Creates a new instance of BookmarkQurery */
    public BookmarkQuery(boolean saveBookmarks) {
        this.queryType = saveBookmarks;
        JabberDataBlock request = new Iq(null, (saveBookmarks) ? Iq.TYPE_SET : Iq.TYPE_GET, "getbookmarks");
        JabberDataBlock query = request.addChildNs("query", "jabber:iq:private");

        JabberDataBlock storage = query.addChildNs("storage", "storage:bookmarks");
        if (saveBookmarks) {
            for (Enumeration e = sd.account.bookmarks.elements(); e.hasMoreElements();) {
                storage.addChild(((Bookmark) e.nextElement()).constructBlock());
            }
        }
        sd.theStream.send(request);
    }

    public int blockArrived(JabberDataBlock data) {
        if (data instanceof Iq) {
            if (data.getAttribute("id").equals("getbookmarks")) {
                String type = data.getTypeAttribute();
                if (type.equals("result")) {
                    JabberDataBlock query = data.findNamespace("query", "jabber:iq:private");
                    if (query != null) {
                        JabberDataBlock storage = query.findNamespace("storage", "storage:bookmarks");
                        if (storage != null) {
                            Vector bookmarks = new Vector();
                            boolean autojoin = cf.autoJoinConferences && sd.roster.myStatus != Presence.PRESENCE_INVISIBLE;
//#ifdef PRIVACY 
//#                      if (!sd.account.isGoogle)
//#                         if (QuickPrivacy.conferenceList == null)
//#                             QuickPrivacy.conferenceList = new Vector();
//#endif                     
                            Vector items = storage.getChildBlocks();
                            if (items != null) {
                                for (Enumeration e = items.elements(); e.hasMoreElements();) {
                                    Bookmark bm = new Bookmark((JabberDataBlock) e.nextElement());
                                    if (bm.nick == null) {
                                        bm.nick = sd.account.nick;
                                    }
//#ifdef PRIVACY                                                
//#                            if (!sd.account.isGoogle) {
//#                              int at = bm.jid.indexOf("@") + 1;
//#                              String host = bm.jid.substring(at, bm.jid.length());
//#                              if (!QuickPrivacy.conferenceList.contains(host)) {
//#                                  QuickPrivacy.conferenceList.addElement(host);
//#                              }
//#                                     }
//#endif                        
                                    if (bm.name == null) {
                                        bm.name = bm.jid;
                                    }
                                    bookmarks.addElement(bm);
                                    if (queryType == LOAD) {
                                        if (bm.autojoin && autojoin) {
                                            Conference.join(bm.name, bm.jid + '/' + bm.nick, bm.password, bm.nick, cf.confMessageCount);
                                        }
                                    }
                                }
//#ifdef PRIVACY                                                
//#                                 if (!sd.account.isGoogle)
//#                                     new QuickPrivacy().updateQuickPrivacyList();
//#endif                        
                            }

                            if (bookmarks.isEmpty()) {
                                loadDefaults(bookmarks);
                            }
                            for (Enumeration e = bookmarks.elements(); e.hasMoreElements();) {
                                sd.account.bookmarks.addElement((Bookmark) e.nextElement());
                            }
                            return JabberBlockListener.NO_MORE_BLOCKS;
                        }
                    }
                }
            }
        }
        return JabberBlockListener.BLOCK_REJECTED;
    }

    private void loadDefaults(Vector bookmarks) {
        Vector defs[] = new StringLoader().stringLoader("/def_bookmarks.txt", 4);
        int j = defs[0].size();
        for (int i = 0; i < j; i++) {
            String jid = (String) defs[0].elementAt(i);
            String nick = (String) defs[1].elementAt(i);
            String pass = (String) defs[2].elementAt(i);
            String desc = (String) defs[3].elementAt(i);
            if (desc == null) {
                desc = jid;
            }
            if (pass == null) {
                pass = "";
            }
            if (nick == null) {
                nick = sd.account.getNickName();
            }
            Bookmark bm = new Bookmark(desc, jid, nick, pass, false);
            bookmarks.addElement(bm);
        }
    }    
}
