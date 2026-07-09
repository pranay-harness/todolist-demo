from langchain_openai import ChatOpenAI
from dotenv import load_dotenv

load_dotenv()

model = ChatOpenAI(model = "gpt-4" , temperature=0 , max_completion_tokens=100 )

result = model.invoke("Give me 5 players of the CSK team")

print(result.content)
