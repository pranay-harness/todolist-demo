import sys
from pathlib import Path

from langchain_openai import ChatOpenAI
from dotenv import load_dotenv
import streamlit as st

# Same-directory import when `streamlit run prompts/prompts.py` from project root
sys.path.insert(0, str(Path(__file__).resolve().parent))
from prompt_generator import template

load_dotenv()

# Align with selected explanation length (100 tokens is too small for "Long")
LENGTH_MAX_TOKENS = {
    "Short (1-2 paragraphs)": 500,
    "Medium (3-5 paragraphs)": 1500,
    "Long (detailed explanation)": 4000,
}

st.header("Research tool")

paper_input = st.selectbox(
    "Select Research Paper Name",
    [
        "Attention Is All You Need",
        "BERT: Pre-training of Deep Bidirectional Transformers",
        "GPT-3: Language Models are Few-Shot Learners",
        "Diffusion Models Beat GANs on Image Synthesis",
    ],
)

style_input = st.selectbox(
    "Select Explanation Style",
    ["Beginner-Friendly", "Technical", "Code-Oriented", "Mathematical"],
)

length_input = st.selectbox(
    "Select Explanation Length",
    ["Short (1-2 paragraphs)", "Medium (3-5 paragraphs)", "Long (detailed explanation)"],
)

user_input = st.text_input(
    "Optional: extra focus, questions, or constraints (e.g. “emphasize attention mechanism”):",
    "",
)

if st.button("Summarise"):
    max_tokens = LENGTH_MAX_TOKENS[length_input]
    model = ChatOpenAI(
        model="gpt-4",
        temperature=0,
        max_completion_tokens=max_tokens,
    )
    base_prompt = template.format(
        paper_input=paper_input,
        style_input=style_input,
        length_input=length_input,
    )
    if user_input.strip():
        full_prompt = (
            f"{base_prompt}\n\n---\n"
            f"Additional instructions from the user:\n{user_input.strip()}"
        )
    else:
        full_prompt = base_prompt

    with st.spinner("Generating summary…"):
        try:
            result = model.invoke(full_prompt)
            st.write(result.content)
        except Exception as e:
            st.error(f"Request failed: {e}")

