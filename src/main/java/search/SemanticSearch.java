package search;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.Console;
import java.io.File;
import java.util.List;
import java.util.Set;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;
import static utilities.Common.VDB_NAME;

public class SemanticSearch {
    private static String query;

    public static void main(String[] args) {

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

        String pstring = "\nString> ";

        Set<String> set = Set.of("exit", "quit", "bye");
        Console console = System.console();

        while (true) {
            query = console.readLine(pstring);
            if (set.contains(query.toLowerCase()))
                break;

            queryEmbedding = emodel.embed(query).content();
            EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(10)
                    .minScore(.6)
                    .build();
            EmbeddingSearchResult<TextSegment> searchResult = myDB.search(embeddingSearchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            for (EmbeddingMatch<TextSegment> embeddingMatch : matches) {
                System.out.println(embeddingMatch.embedded().text());
                System.out.print("["+embeddingMatch.embedded().metadata().getInteger("lineNumber") + ": ");
                System.out.print(embeddingMatch.embedded().metadata().getString("source")+": ");
                System.out.println(embeddingMatch.score());
                System.out.println("=========================================================================");

            }

            //EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);
            //TextSegment ts = embeddingMatch.embedded();
            //System.out.println(ts.metadata());
        }
    }
}
