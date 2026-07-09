from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import JsonOutputParser
from dotenv import load_dotenv
from typing import TypedDict
import os

load_dotenv()

model = ChatOpenAI(model="gpt-4", temperature=0, max_completion_tokens=100)

class Review(TypedDict):
    summary: str
    sentiment: int



structured_llm = model.with_structured_output(Review)

result = structured_llm.invoke("I’ve been using these earbuds for about 3 weeks now, mostly during workouts and daily commuting. The sound quality is surprisingly clear for the price, with decent bass and good noise isolation. Battery life lasts close to 6–7 hours on a single charge, which matches the product description.

The earbuds pair quickly with my phone and reconnect automatically every time I open the case. The fit is comfortable, and they don’t fall out while running.

One small downside is that the touch controls are a little sensitive, so I accidentally pause music sometimes when adjusting them. Overall, excellent value for money and I would definitely recommend them.

This matches the style commonly seen in Amazon reviews: headline, star rating, pros, cons, and real usage experience.")


print(result)

parser = JsonOutputParser(pydantic_object=Review)
chain = model | parser
json_result = chain.invoke("Analyze this review and return summary and sentiment (1-5): Great earbuds with clear sound and good battery life.")
print(json_result)
