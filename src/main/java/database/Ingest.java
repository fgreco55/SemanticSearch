package database;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;
import static utilities.Common.VDB_NAME;

public class Ingest {
    public static void main(String[] args) {
        //String dirname = args[1];
        String dirname = "src/main/";       // hard-coded just for educational purposes
        Ingest ingest = new Ingest();

        System.err.println("PWD ->" + System.getProperty("user.dir"));      // just for teaching purposes

        InMemoryEmbeddingStore<TextSegment> estore = new InMemoryEmbeddingStore<>();

        List<Document> mydocs = ingest.loadDocuments(estore, dirname);
        //ingest.showDocs(mydocs);
        System.out.println("Loaded " + mydocs.size() + " documents into the EmbeddingStore [" + VDB_NAME + "]");

        ingest.mergeAndPersistEmbeddingStores(estore, VDB_NAME);
    }

    /**
     * loadDocuments(EmbeddingStore<TextSegment> estore, String baseDirectory, boolean loadDocs)
     * @param estore - Reference to a EmbeddingStore
     * @param dirname - basedir to
     * @return List of all the loaded/chunked documents
     */
    List<Document> loadDocuments(EmbeddingStore<TextSegment> estore, String dirname) {
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()  // move to later
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .build();

        // Create a recursive Splitter.  Note: this "recursive" is different from "loadDocumentsRecursively"
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);

        List<Document> allDocuments = new ArrayList<>();

        // --- Load TXT files ---
        List<Document> txtDocs = FileSystemDocumentLoader.loadDocumentsRecursively(dirname, glob("{*.txt,**/*.txt}"), new TextDocumentParser());
        addMetadataAndStore(txtDocs, estore, splitter, embeddingModel, allDocuments);

        // --- Load PDF files ---
        List<Document> pdfDocs = FileSystemDocumentLoader.loadDocumentsRecursively(dirname, glob("{*.pdf,**/*.pdf}"), new ApachePdfBoxDocumentParser());
        addMetadataAndStore(pdfDocs, estore, splitter, embeddingModel, allDocuments);

        return allDocuments;
    }

    /**
     * addMetadataAndStore() - split, get the embeddings and store each document
     * @param docs
     * @param store
     * @param splitter
     * @param embeddingModel
     * @param collector
     */
    private static void addMetadataAndStore(List<Document> docs,
                                            EmbeddingStore<TextSegment> store,
                                            DocumentSplitter splitter,
                                            EmbeddingModel embeddingModel,
                                            List<Document> collector) {
        for (Document doc : docs) {
            String fileName = doc.metadata().getString("file_name"); // FileSystemDocumentLoader sets "source" path???

            collector.add(doc);

            // Split and embed
            List<TextSegment> segments = splitter.split(doc);
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                store.add(embedding, segment);
            }

            // (Optional) Log ingestion
            System.out.println("Ingested file: " + fileName);
        }
    }

    /**
     *
     * @param glob(String s) - match filenames based on glob-string (global search filename-matching string)
     * @return
     */
    public static PathMatcher glob(String glob) {
        return FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

    /**
     * merge(existing, new) - merge the old embedding-store with the new embedding-store
     * @param existing
     * @param newDB
     * @return
     */
    private InMemoryEmbeddingStore<TextSegment> merge(InMemoryEmbeddingStore<TextSegment> existing, InMemoryEmbeddingStore<TextSegment> newDB) {
        if (existing == null) {
            return newDB;
        } else if (newDB == null) {
            return existing;
        } else if (existing == newDB) {     // If same, just return the new one.
            return newDB;
        } else {
            return InMemoryEmbeddingStore.merge(existing, newDB);   // If both are legit EmbeddingStores, then merge them
        }
    }

    /**
     * mergeAndPersistEmbeddingStores() - merge and save the embedding-store
     * @param newEmbeddingStore
     * @param filePathOriginalEmbeddingStore
     */
    public void mergeAndPersistEmbeddingStores(InMemoryEmbeddingStore<TextSegment> newEmbeddingStore, String filePathOriginalEmbeddingStore) {

        File origEmbeddingFile = new File(filePathOriginalEmbeddingStore);

        if (!origEmbeddingFile.exists() || origEmbeddingFile.length() == 0) {   // if orig embedding store doesn't exist or 0 length, just serialize new one
            newEmbeddingStore.serializeToFile(filePathOriginalEmbeddingStore);
        } else {
            InMemoryEmbeddingStore<TextSegment> orig = InMemoryEmbeddingStore.fromFile(VDB_NAME);
            merge(orig, newEmbeddingStore).serializeToFile(filePathOriginalEmbeddingStore);
        }
    }

    /**
     * showDocs() - display the strings and metadata of all the Documents
     * @param mydocs - the list of Documents to be stored in the EmbeddingStore
     */
    public void showDocs(List<Document> mydocs) {
        for  (Document document : mydocs) {
            System.out.print(document.text());                // just show what text was saved in the EmbeddingStore
            System.out.println(document.metadata().toString()); // display the metadata for each string stored
        }
    }
}
