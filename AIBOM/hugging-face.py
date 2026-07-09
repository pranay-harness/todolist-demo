import os
from langchain_huggingface import HuggingFaceEndpoint

llm = HuggingFaceEndpoint(
    repo_id="TinyLlama/TinyLlama-1.1B-Chat-v1.0",
    huggingfacehub_api_token=os.getenv("HUGGINGFACE_API_TOKEN"),
    task="conversational",  # IMPORTANT for chat models
    temperature=0.7,
    max_new_tokens=200
)

messages = [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Explain SBOM in simple terms."}
]

response = llm.invoke(messages)
print(response)
