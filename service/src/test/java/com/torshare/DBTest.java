package com.torshare;

import com.frostwire.jlibtorrent.TorrentInfo;
import com.torshare.db.Actions;
import com.torshare.db.Tables;
import com.torshare.tools.DataSources;
import com.torshare.tools.Tools;
import com.torshare.torrent.LibtorrentEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.NoSuchElementException;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by tyler on 12/4/16.
 */
public class DBTest {

    File ubuntuTorrent = new File(DataSources.UBUNTU_TORRENT);
    LibtorrentEngine lte;


    @Before
    public void setUp() throws Exception {
        lte = LibtorrentEngine.INSTANCE;
        Tools.dbInit();
    }

    @After
    public void tearDown() throws Exception {
        Tools.dbClose();
    }

    @Test
    public void readTorrent() throws Exception {

        TorrentInfo ti = new TorrentInfo(ubuntuTorrent);
        assertEquals("ubuntu-16.10-desktop-amd64.iso", ti.name());
    }


    @Test(expected=NoSuchElementException.class)
    public void testAlreadyExists() throws Exception {
        TorrentInfo ti = new TorrentInfo(ubuntuTorrent);
        Actions.saveTorrentInfo(ti);
        Actions.saveTorrentInfo(ti);
    }

    @Test
    public void testSaveTorrentInfo() throws Exception {
        TorrentInfo ti = new TorrentInfo(ubuntuTorrent);

        Tables.Torrent t = Tables.Torrent.findFirst("info_hash = ?", ti.infoHash().toString());
        if (t != null) t.delete();

        t = Actions.saveTorrentInfo(ti);

        assertEquals("ubuntu-16.10-desktop-amd64.iso", t.getString("name"));

        t.delete();
    }

    public void fetchMagnetURI() throws Exception {

        String uri = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c";

        LibtorrentEngine lte = LibtorrentEngine.INSTANCE;

        lte.fetchMagnetURI(uri);
    }

    @Test
    public void testBencode() throws Exception {

        TorrentInfo ti = new TorrentInfo(ubuntuTorrent);
        Tables.Torrent t = Actions.saveTorrentInfo(ti);

        byte[] data = t.getBytes("bencode");

//        System.out.println(Entry.bdecode(data));

        TorrentInfo ti_2 = TorrentInfo.bdecode(data);

        assertEquals("ubuntu-16.10-desktop-amd64.iso", ti_2.name());

        t.delete();
    }

    @Test
    public void asdf() throws Exception {
        TorrentInfo ti = new TorrentInfo(ubuntuTorrent);
        lte.addTorrent(ti);
    }


}
