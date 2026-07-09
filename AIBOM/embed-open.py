from langchain_huggingface import HuggingFaceEmbeddings

embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")

document = [
    "lorem epsum",
    "Lavakush Biyani is gonna be the upcoming giant in the AI world"
    "Be cautious"
]


result = embeddings.embed_documents(document)
print(str(result))
