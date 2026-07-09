from langchain_openai import OpenAIEmbeddings

from dotenv import load_dotenv
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np



load_dotenv()

embeddings = OpenAIEmbeddings(model="text-embedding-3-small", dimensions=1536)

document = [

"Virat Kohli – The modern run-machine known for his consistency and chasing ability",

"Rohit Sharma – The Hitman with a record three ODI double centuries",

"MS Dhoni – Captain Cool who led India to all three major ICC trophies",

"Sachin Tendulkar – The God of Cricket with 100 international centuries.",

"Kapil Dev – The 1983 World Cup-winning captain and legendary all-rounder.",
]

query = "give me info about sachin"

doc_embedding  = embeddings.embed_documents(document)
query_embedding = embeddings.embed_query(query)

scores = cosine_similarity([query_embedding], doc_embedding)[0]

index, score = sorted(list(enumerate(scores)), key=lambda x: x[1])[-1]

print(query)

print(document[index])
print("similarity score is", score)

