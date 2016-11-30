package retrieval_system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.LegacyLongField;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

@SuppressWarnings("deprecation")
public class FileIndexer {

	/**
	 * This is the Analyzer used for indexing and searching. It uses porter stemmer.
	 */
	private Analyzer mAnal = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
			.addTokenFilter(StandardFilterFactory.class).addTokenFilter(LowerCaseFilterFactory.class)
			.addTokenFilter(StopFilterFactory.class).addTokenFilter(PorterStemFilterFactory.class).build();

	/**
	 * This is a writer for the index.
	 */
	private IndexWriter mWriter;

	/**
	 * Contains the path to our index.
	 */
	private String indexPath = "";

	/**
	 * The constructor of our class, initializes needed variables.
	 * <p> 
	 * The constructor gets the absolute path to the .jar file being run
	 * and sets it as the index directory. Also initializes other needed
	 * members like the mWriter.
	 */
	
	FileIndexer() throws IOException {
		// getting filepath to our jar file
		File f = new File(System.getProperty("java.class.path"));
		File filePath = f.getAbsoluteFile().getParentFile();
		indexPath = filePath.toString();
		FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
		IndexWriterConfig config = new IndexWriterConfig(mAnal);

		mWriter = new IndexWriter(dir, config);
		mWriter.deleteAll();
	}

	/**
	 * Indexes "all" files in the specified URI directory.
	 * The directory argument must specify an absolute {@link URI}.
	 * <p> 
	 * This method crawls trough all folders in the specified directory
	 * and takes all files ending with .TXT or .HTML and indexes them.
	 * For HTML files it looks for the first occurrence of the tag
	 * "summary" and saves the containing text in the datatype Document for the 
	 * specific file. The same is done for the first occurrence of a "time" tag
	 * with the attribute "datetime" e.g. &lt;time datetime="1914"&gt;
	 * It then save the value of the attribute in the same manner as it is 
	 * done for the summary tag
	 *
	 * @param  directory the URI to the directory which contains the files to be indexed
	 */
	public void indexDirectory(String directory) throws IOException {
		File dir = new File(directory);
		for (File curFile : dir.listFiles()) {
			if (curFile.isDirectory()) {
				this.indexDirectory(curFile.getAbsolutePath());
			} else {
				String filename = curFile.getName().toLowerCase();
				if (filename.endsWith(".txt") || filename.endsWith(".html")) {
					Document doc = new Document();
					
					if(filename.endsWith(".html")) {
						org.jsoup.nodes.Document doc_jsoup = Jsoup.parse(curFile, "UTF-8", "");
						Element sum = doc_jsoup.select("summary").first();
						Element dat = doc_jsoup.select("time[datetime]").first();
						
						doc.add(new TextField("contents", doc_jsoup.text(), Field.Store.YES));
						doc.add(new TextField("title", doc_jsoup.title(), Field.Store.YES));
						if(sum != null){
							doc.add(new TextField("summary", sum.text() , Field.Store.YES));
						}
						else{
							doc.add(new TextField("summary", "", Field.Store.YES));
						}
						
						if(dat != null){
							doc.add(new TextField("date", dat.attr("datetime"), Field.Store.YES));
						}
						else{
							doc.add(new TextField("date", "", Field.Store.YES));
						}

					}
					else{
						doc.add(new TextField("contents", new FileReader(curFile)));
						doc.add(new TextField("title", "", Field.Store.YES));
						doc.add(new TextField("summary", "", Field.Store.YES));
						doc.add(new TextField("date", "", Field.Store.YES));
					}
					
					doc.add(new StringField("path", curFile.getPath(), Field.Store.YES));
					doc.add(new StringField("filename", curFile.getName(), Field.Store.YES));
					doc.add(new LegacyLongField("lastmodified", curFile.lastModified(), Field.Store.YES));
					
					mWriter.addDocument(doc);
					System.out.println("Added: " + curFile.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Searches for the specified query in our created index.
	 * The searchquery argument must specify a string to be looked for.
	 * <p> 
	 * This method looks for the specified string query in our index and prints out
	 * all files containg this file with additional information.
	 * For all files it returns the Rank, Path to the file, the Score 
	 * and the time it was last modified.
	 * Additionally for HTML files it returns the Title and a Summary.
	 *
	 * @param  searchquery the String which shall be looked for in the created index
	 */
	public void search(String searchquery) throws IOException, ParseException {

		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(5);

		Query query = new QueryParser("contents", mAnal).parse(searchquery);
		searcher.search(query, collector);
		query = new QueryParser("title", mAnal).parse(searchquery);
		searcher.search(query, collector);
		query = new QueryParser("date", mAnal).parse(searchquery);
		searcher.search(query, collector);
		
		
		ScoreDoc[] scores = collector.topDocs().scoreDocs;
		System.out.println("Found " + scores.length + " documents.");
		
		//rank, path, relevance score, last modification time
		if(scores.length > 0) {
			System.out.println("Rank <|> Path <|> Score <|> Last Modified <|> Title <|> Summary");
		}
		for (int i = 0; i < scores.length; ++i) {
			int docId = scores[i].doc;
			Document d = searcher.doc(docId);
			long datelong = Long.parseLong(d.get("lastmodified"));
			String dateString = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(datelong));
			
			System.out.println((i + 1) + ". <|> " + d.get("path") + " <|> " + scores[i].score 
					+ " <|> " + dateString+ " <|> " + d.get("title") + " <|> " + d.get("summary"));
		}

	}
	
	/**
	 * Creates an objects of our FileIndexer class and uses it for indexing and searching.
	 * The args argument must contain the directory as absolute URI at first place where the files are that shall be indexed.
	 * <p> 
	 * Creates an object of the FileIndexer class, takes the directory from the args argument
	 * and uses it with a method to index all files.
	 * Then it takes input as a search query and uses a method to search for the query in our index.
	 * 
	 * @param  args the directory at first place where the files are that shall be indexed.
	 */
	public static void main(String[] args) throws IOException, ParseException {
		FileIndexer indexer = new FileIndexer();
		indexer.indexDirectory(args[0]);
		System.out.println("Indexed " + indexer.mWriter.numDocs() + " documents.");
		indexer.mWriter.close();

		BufferedReader bufreader = new BufferedReader(new InputStreamReader(System.in));
		String queryInput = "";
		while (!queryInput.equalsIgnoreCase("yx")) {

			System.out.println("\nSearch Query (yx = exit):");
			queryInput = bufreader.readLine();
			if(queryInput.startsWith(" ")){
				System.out.println("Error: Wrong Input!");
			}
			else if (queryInput.equalsIgnoreCase("yx")) {
				break;
			}
			else {
				indexer.search(queryInput);
			}
		}

	}
}
