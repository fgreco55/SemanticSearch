package database;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;
import static utilities.Common.VDB_NAME;

public class Ingestor {

    public static void main(String[] args) throws IOException {
        Ingestor ingestor = new Ingestor();

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()  // move to later
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .build();

        String pstring = "\nCmd> ";

        Set<String> set = Set.of("exit", "quit", "bye");
        Console console = System.console();

        while (true) {
            String filename = console.readLine(pstring);
            if (set.contains(filename.toLowerCase()))
                break;
            System.out.println("File to ingest: " + filename);
            ingestFile(embeddingStore, embeddingModel, filename);
        }
        ingestor.mergeAndPersistEmbeddingStores(embeddingStore, VDB_NAME);
    }

    public static void ingestFile(InMemoryEmbeddingStore<TextSegment> estore, EmbeddingModel emodel, String filepath) throws IOException {
        Path filePath = Path.of(filepath);
        String content = Files.readString(filePath);

        // Regex to split sentences by '.', '!', or '?' followed by whitespace
        final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");     // didn't like LC4J Sentence splitter - fdg
        String[] sentences = SENTENCE_PATTERN.split(content);

        int lineno = 0;
        for (String sentence : sentences) {
            sentence = sentence.trim();
            Embedding embedding = emodel.embed(sentence).content(); // get embedding vector

            Metadata metadata = Metadata.from(Map.of(           // create metadata
                    "lineNumber", lineno++,
                    "source", filePath.toFile().getAbsolutePath()
            ));
            TextSegment ts = TextSegment.from(sentence, metadata);
            System.err.println("SENTENCE: " + ts);
            estore.add(embedding, ts);
        }
    }

    private InMemoryEmbeddingStore<TextSegment> merge(InMemoryEmbeddingStore<TextSegment> existing, InMemoryEmbeddingStore<TextSegment> newDB) {
        if (existing == null) {
            return newDB;
        } else if (newDB == null) {
            return existing;
        } else if (existing == newDB) {     // If same, just return the new one.
            return newDB;
        } else {
            return InMemoryEmbeddingStore.merge(existing, newDB);
        }
    }

    public void mergeAndPersistEmbeddingStores(InMemoryEmbeddingStore<TextSegment> newEmbeddingStore, String filePathOriginalEmbeddingStore) {

        File origEmbeddingFile = new File(filePathOriginalEmbeddingStore);

        if (!origEmbeddingFile.exists() || origEmbeddingFile.length() == 0) {   // if orig embedding store doesn't exist or 0 length, just serialize new one
            newEmbeddingStore.serializeToFile(filePathOriginalEmbeddingStore);
        } else {
            InMemoryEmbeddingStore<TextSegment> orig = InMemoryEmbeddingStore.fromFile(VDB_NAME);
            merge(orig, newEmbeddingStore).serializeToFile(filePathOriginalEmbeddingStore);
        }
    }
}