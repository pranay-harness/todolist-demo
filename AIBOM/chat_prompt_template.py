from langchain_core.prompts import ChatPromptTemplate

chat_prompt_template = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant."),
    ("user", "{input}"),
])

result = chat_prompt_template.invoke({"input": "Hello, how are you?"})

print(result)