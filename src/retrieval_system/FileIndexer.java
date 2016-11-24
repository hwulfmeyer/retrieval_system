package retrieval_system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;

public class FileIndexer {

	private Analyzer mAnal = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
			.addTokenFilter(StandardFilterFactory.class).addTokenFilter(LowerCaseFilterFactory.class)
			.addTokenFilter(StopFilterFactory.class).addTokenFilter(PorterStemFilterFactory.class).build();

	private IndexWriter mWriter;

	private String indexPath = "";

	FileIndexer() throws IOException {
		// getting filepath to our jar file
		File f = new File(System.getProperty("java.class.path"));
		File filePath = f.getAbsoluteFile().getParentFile();
		String path = filePath.toString();
		path = "c:\\Users\\hyperion\\Documents\\GitHub\\retrieval_system\\documents";
		indexPath = path;
		FSDirectory dir = FSDirectory.open(Paths.get(path));
		IndexWriterConfig config = new IndexWriterConfig(mAnal);

		mWriter = new IndexWriter(dir, config);
		mWriter.deleteAll();
	}

	public void indexDirectory(String directory) throws IOException {
		File dir = new File(directory);
		for (File curFile : dir.listFiles()) {
			if (curFile.isDirectory()) {
				this.indexDirectory(curFile.getAbsolutePath());
			} else {
				String filename = curFile.getName().toLowerCase();
				if (filename.endsWith(".txt") || filename.endsWith(".html")) {
					
					Document doc = new Document();
					doc.add(new TextField("contents", new FileReader(curFile)));
					doc.add(new StringField("path", curFile.getPath(), Field.Store.YES));
					doc.add(new StringField("filename", curFile.getName(), Field.Store.YES));
					
					mWriter.addDocument(doc);
					System.out.println("Added: " + curFile.getAbsolutePath());
				}
			}
		}
	}


	public void search(String searchquery) throws IOException, ParseException {

		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(5);

		Query query = new QueryParser("contents", mAnal).parse(searchquery);
		searcher.search(query, collector);
		ScoreDoc[] scores = collector.topDocs().scoreDocs;

		System.out.println("Found " + scores.length + " hits.");
		for (int i = 0; i < scores.length; ++i) {
			int docId = scores[i].doc;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". " + d.get("path") + " score=" + scores[i].score);
		}

	}

	public static void main(String[] args) throws IOException, ParseException {
		FileIndexer indexer = new FileIndexer();
		indexer.indexDirectory("c:\\Users\\hyperion\\Documents\\GitHub\\retrieval_system\\documents\\plain_text");
		System.out.println("Indexed " + indexer.mWriter.numDocs() + " documents.");
		indexer.mWriter.close();

		BufferedReader bufreader = new BufferedReader(new InputStreamReader(System.in));
		String queryInput = "";
		while (!queryInput.equalsIgnoreCase("yx")) {

			System.out.println("Search Query (yx = exit):");
			queryInput = bufreader.readLine();

			if (queryInput.equalsIgnoreCase("yx")) {
				break;
			}
			indexer.search(queryInput);
		}

	}
}
