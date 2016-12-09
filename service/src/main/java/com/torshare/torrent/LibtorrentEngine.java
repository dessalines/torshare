package com.torshare.torrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.torshare.db.Actions;
import com.torshare.db.Tables;
import com.torshare.tools.DataSources;
import com.torshare.tools.Tools;
import org.apache.commons.io.FileUtils;
import org.javalite.activejdbc.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by tyler on 12/2/16.
 */
public enum LibtorrentEngine {

    INSTANCE;

    private Logger log = LoggerFactory.getLogger(LibtorrentEngine.class);

    private SessionManager s;

    LibtorrentEngine() {

        System.setProperty("jlibtorrent.jni.path", DataSources.LIBTORRENT_PATH);
        log.info("Starting up libtorrent with version: " + LibTorrent.version());

        s = new SessionManager();

        dhtBootstrap();

        s.addListener(alerts());

        s.maxActiveDownloads(-1);
        s.maxActiveSeeds(-1);
        s.downloadRateLimit(10000);

    }

    public void addTorrent(TorrentInfo ti) throws IOException {

        log.info("Added torrent: " + ti.name());

        Path tempDir = Files.createTempDirectory("tmp");
        Tools.recursiveDeleteOnShutdownHook(tempDir);

        this.s.download(ti, tempDir.toFile());

//        log.info("temp dir: " + tempDir.toAbsolutePath().toString());

    }

    public byte[] fetchMagnetURI(String uri) {

        String infoHash = uri.split("btih:")[1].substring(0, 40);

        Tools.dbInit();
        Tables.Torrent torrent = Tables.Torrent.findFirst("info_hash = ?", infoHash);


        if (torrent != null) {
            throw new NoSuchElementException("Torrent Already Added: " + infoHash);
        }
        Tools.dbClose();

        log.info("Fetching the magnet uri, please wait...");
        byte[] data = s.fetchMagnet(uri, Integer.MAX_VALUE);


        if (data != null) {
            log.info(Entry.bdecode(data).toString());
        } else {
            throw new NoSuchElementException("Failed to retrieve the magnet:" + uri.toString());
        }

        return data;

    }

    public void addTorrentsOnStartup() throws IOException {

        Tools.dbInit();

        LazyList<Tables.Torrent> torrents = Tables.Torrent.find("bencode is not null");

        for (Tables.Torrent t : torrents) {
            byte[] data = t.getBytes("bencode");
            addTorrent(TorrentInfo.bdecode(data));
        }

        Tools.dbClose();
    }

    private AlertListener alerts() {

        return new AlertListener() {
            @Override
            public int[] types() {
//                return null;
                return new int[]{
                        AlertType.TORRENT_ADDED.swig(),
                        AlertType.TRACKER_REPLY.swig(),
                        AlertType.DHT_REPLY.swig(),
                        AlertType.METADATA_RECEIVED.swig()
                };

            }


            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                log.info(alert.what());
                log.info(alert.message());

                switch (type) {
                    case TORRENT_ADDED:
                        TorrentAddedAlert a = (TorrentAddedAlert) alert;
                        a.handle().resume();
                        a.handle().setAutoManaged(false);
                        break;

                    case TRACKER_REPLY:
                        TrackerReplyAlert tra = (TrackerReplyAlert) alert;
                        log.info("list peers count: " + tra.handle().status().listPeers());
                        break;

                    case DHT_REPLY:
                        DhtReplyAlert dhtReply = (DhtReplyAlert) alert;
                        log.info("dht reply peers: " + dhtReply.numPeers());
                        log.info("num peers: " + dhtReply.handle().status().numPeers());
                        log.info("num seeds: " + dhtReply.handle().status().numSeeds());
                        log.info("list peers count: " + dhtReply.handle().status().listPeers());
                        log.info("list seeds count: " + dhtReply.handle().status().listSeeds());
                        log.info("num complete: " + dhtReply.handle().status().numComplete());
                        log.info("num incomplete: " + dhtReply.handle().status().numIncomplete());
                        int peers = dhtReply.handle().status().listPeers();
                        int seeds = dhtReply.handle().status().listSeeds();

                        if (!dhtReply.handle().name().startsWith("fetch_magnet___magnet") && peers != 0) {
                            Tools.dbInit();
                            Actions.saveSeeders(dhtReply.handle().infoHash().toString(), seeds, peers);
                            Tools.dbClose();
                        }

                        break;
                    case METADATA_RECEIVED:
                        MetadataReceivedAlert mar = (MetadataReceivedAlert) alert;
                        byte[] data = mar.torrentData();
                        log.info(data.toString());

                        TorrentInfo ti = TorrentInfo.bdecode(data);
                        try {
                            Tools.dbInit();
                            Actions.saveTorrentInfo(ti);
                            Tools.dbClose();
                            addTorrent(ti);
                        } catch (IOException e) {}


                        break;

                }
            }
        };
    }

    private void dhtBootstrap() {

        log.info("Bootstrapping DHT nodes...");
        final CountDownLatch signal = new CountDownLatch(1);

        // the session stats are posted about once per second.
        AlertListener l = new AlertListener() {
            @Override
            public int[] types() {
                return new int[]{AlertType.SESSION_STATS.swig(), AlertType.DHT_STATS.swig()};
            }

            @Override
            public void alert(Alert<?> alert) {

                if (alert.type().equals(AlertType.SESSION_STATS)) {
                    s.postDhtStats();
                }

                if (alert.type().equals(AlertType.DHT_STATS)) {

                    long nodes = s.stats().dhtNodes();
                    // wait for at least 10 nodes in the DHT.
                    if (nodes >= 10) {
                        signal.countDown();
                    }
                }
            }
        };

        s.addListener(l);
        s.start();
        s.postDhtStats();

        // waiting for nodes in DHT (10 seconds)
        boolean r = false;
        try {
            r = signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }


        // no more trigger of DHT stats
        s.removeListener(l);

        log.info("Done bootstrapping");
    }

}
