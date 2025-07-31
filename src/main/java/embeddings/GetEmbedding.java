package embeddings;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;

public class GetEmbedding {
    public static void main(String[] argv) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .build();

        Response<Embedding> response = model.embed("One fish, two fish, red fish, blue fish");
        Embedding embedding = response.content();

        System.out.println(embedding.dimension() + " elements: " + embedding);
    }
}

