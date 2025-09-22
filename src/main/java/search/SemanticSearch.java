package search;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;
import static utilities.Common.VDB_NAME;



public class SemanticSearch {
    private final static String configFile = "src/main/resources/SemanticSearch.properties";

    public static void main(String[] args) throws IOException {

        File embeddingfile = new File(VDB_NAME);

        if (!embeddingfile.exists()) {
            System.err.println("Embedding Store [" + VDB_NAME + "] does not exist.");
            System.exit(0);
        } else if (embeddingfile.length() == 0) {
            System.err.println("Embedding Store [" + VDB_NAME + "] has 0 length.");
            System.exit(0);
        }

        InMemoryEmbeddingStore<TextSegment> myDB = InMemoryEmbeddingStore.fromFile(VDB_NAME);
        Embedding queryEmbedding;

        EmbeddingModel emodel = OpenAiEmbeddingModel.builder()  // move to later
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .build();

        String pstring = "String> ";

        Set<String> set = Set.of("exit", "quit", "bye");
        Console console = System.console();

        Properties prop = SetProperties(configFile);
        Integer maxResults = Integer.parseInt(prop.getProperty("SemanticSearch.maxResults"));
        Double minScore = Double.parseDouble(prop.getProperty("SemanticSearch.minScore"));
        boolean verbose = Boolean.parseBoolean(prop.getProperty("SemanticSearch.verbose"));

        while (true) {
            String query = console.readLine(pstring);
            if (set.contains(query.toLowerCase()))
                break;

            queryEmbedding = emodel.embed(query).content();
            EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(minScore)
                    .build();
            EmbeddingSearchResult<TextSegment> searchResult = myDB.search(embeddingSearchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            displayMatches(matches, verbose);
        }
    }

    /**
     * displayMatches(matches, verbose)
     *
     * @param matches - List of matches returned from the EmbeddingStore
     * @param verbose - When verbose is true, show score and matched text
     */
    private static void displayMatches(List<EmbeddingMatch<TextSegment>> matches, boolean verbose) {
        for (EmbeddingMatch<TextSegment> embeddingMatch : matches) {
            System.out.print("["+embeddingMatch.embedded().metadata().getString("absolute_directory_path") + ": ");
            System.out.print(embeddingMatch.embedded().metadata().getString("file_name")+": ");
            if (verbose) {
                System.out.print(" " + embeddingMatch.score());
                System.out.print(" " + embeddingMatch.embedded().text());
            }
            System.out.println();
        }
    }

    /**
     * SetProperties(configFile) - read properties file and create a Properties with the name/values
     * @param configFile - location of the properties file
     * @return loaded Properties
     * @throws IOException
     */
    private static Properties SetProperties(String configFile) throws IOException {
            Properties prop = new Properties();
            InputStream in = new FileInputStream(configFile);

            prop.load(in);

            for (Enumeration e = prop.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement().toString();
                //System.out.println(key + " = " + prop.getProperty(key));
            }
            return prop;
    }
}
