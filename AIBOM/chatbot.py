from langchain_openai import ChatOpenAI
from dotenv import load_dotenv
import streamlit as st

load_dotenv()

model = ChatOpenAI(model="gpt-4", temperature=0, max_completion_tokens=100)

result = model.invoke("Hello, how are you?")

print(result.content)

while True:
    user_input = st.text_input("You:")
    if user_input.lower() == "exit":
        break
    result = model.invoke(user_input)
    print("Chatbot:", result.content)