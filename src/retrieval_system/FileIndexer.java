package retrieval_system;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

public class FileIndexer {

	private static StandardAnalyzer mAnalyzer = new StandardAnalyzer();
	private IndexWriter mWriter;

	FileIndexer() throws IOException {
		// getting filepath to our jar file
		File f = new File(System.getProperty("java.class.path"));
		File filePath = f.getAbsoluteFile().getParentFile();
		String path = filePath.toString();
		path = "c:\\Users\\hyperion\\Documents\\GitHub\\retrieval_system\\documents";

		FSDirectory dir = FSDirectory.open(Paths.get(path));
		IndexWriterConfig config = new IndexWriterConfig(mAnalyzer);

		mWriter = new IndexWriter(dir, config);
	}
	
	public void indexDirectory(String directory) throws IOException {
		File dir = new File(directory);
		for (File curFile: dir.listFiles()) {
			String filename = curFile.getName().toLowerCase();
			
			if (curFile.isDirectory()) {
				this.indexDirectory(curFile.getAbsolutePath());
			}
			else if (filename.endsWith(".txt") || filename.endsWith(".html")) {
				Document doc = new Document();
				doc.add(new TextField("content", new FileReader(curFile)));
				doc.add(new StringField("path", curFile.getPath(), Field.Store.YES));
				mWriter.addDocument(doc);
				
				System.out.println("Added: " + curFile.getAbsolutePath());
			}
		}
	}

	public static void main(String[] args) throws IOException {
		FileIndexer indexer = new FileIndexer();
		indexer.mWriter.deleteAll();
		indexer.indexDirectory("c:\\Users\\hyperion\\Documents\\GitHub\\retrieval_system\\documents\\plain_text");
		System.out.println("Indexed " + indexer.mWriter.numDocs() +" documents.");
		indexer.mWriter.close();
	}
}
