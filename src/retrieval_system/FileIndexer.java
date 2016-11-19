package retrieval_system;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

		FSDirectory dir = FSDirectory.open(Paths.get(path));
		IndexWriterConfig config = new IndexWriterConfig(mAnalyzer);

		mWriter = new IndexWriter(dir, config);
	}

	public static void main(String[] args) throws IOException {
		FileIndexer indexer = new FileIndexer();
		
	}
}
