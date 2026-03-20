from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from fastapi.middleware.cors import CORSMiddleware
import os
from typing import List, Optional

app = FastAPI(title="Unified Student Backend")

# API versioning/prefixes
QR_PREFIX = "/api/qr"

# Enable CORS for local testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class Student(BaseModel):
    id: str  # This is the unique code
    full_name: str
    organization: str
    issue_date: str
    specialty: str
    course: str
    hash: Optional[str] = None

class LoginRequest(BaseModel):
    code: str
    mood: Optional[str] = "neutral"

# Mock Database
students_db = {
    "nFuvUG6qzp3s": Student(
        id="nFuvUG6qzp3s",
        full_name="Погрибной Максим Юрьевич",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="3",
        hash="hash_m_p_2026"
    ),
    "student1": Student(
        id="student1",
        full_name="Иванов Иван Иванович",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2024",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="2",
        hash="hash_i_i_2026"
    ),
    "student2": Student(
        id="student2",
        full_name="Петров Петр Петрович",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2022",
        specialty="09.02.07 «Информационные системы и программирование»",
        course="4",
        hash="hash_p_p_2026"
    ),
    "student3": Student(
        id="student3",
        full_name="Сидоров Сидор Сидорович",
        organization="ГГТУ Промышленно-экономический колледж",
        issue_date="01.09.2023",
        specialty="09.02.06 «Сетевое и системное администрирование»",
        course="3",
        hash="hash_s_s_2026"
    )
}

@app.get("/")
async def root():
    return {"message": "Unified Backend is running"}

@app.post(f"{QR_PREFIX}/login")
async def login(request: LoginRequest):
    if request.code == "debug":
        return students_db["nFuvUG6qzp3s"]
    
    if request.code in students_db:
        return students_db[request.code]
    
    raise HTTPException(status_code=401, detail="Invalid unique code")

@app.get(f"{QR_PREFIX}/student/{{code}}")
async def get_student(code: str):
    if code in students_db:
        return students_db[code]
    raise HTTPException(status_code=404, detail="Student not found")

if __name__ == "__main__":
    import uvicorn
    # Use PORT environment variable for cloud hosting
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
