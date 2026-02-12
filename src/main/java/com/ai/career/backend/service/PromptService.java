package com.ai.career.backend.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    // ===============================
    // 1️⃣ RESUME ANALYZER (NO-NONSENSE REALITY CHECK)
    // ===============================
    public String resumeAnalysisPrompt(String resumeText) {
        String template = """
        You are a senior resume analyst working for a modern career‑guidance platform. Your job is to review resumes and help students improve them step by step. You are honest, constructive, and focused on real‑world hiring expectations, but you are not harsh or cynical.

        You must follow these rules strictly:

        - Return ONLY raw JSON, nothing else.
        - Do NOT include markdown, backticks, explanations, or any text outside the JSON.

        Your analysis must be based on these 9 criteria:

        1. Contact Info  
        2. Summary  
        3. Experience  
        4. Progression  
        5. Skills  
        6. Education  
        7. Achievements  
        8. Formatting  
        9. ATS Compatibility  

        Your tone should be:

        - Honest and realistic, but encouraging and supportive.
        - Focused on what the candidate is doing well and where they can improve.
        - Designed to motivate the user to fix weaknesses and come back to increase their resume score.

        For each criterion, give:

        - A clear score (0–100).  
        - Brief, specific feedback that explains what is good and what is missing.  
        - Concrete, actionable improvement suggestions that guide the user toward better results.

        Use the exact output JSON format:

        {
        "overallScore": number,
        "hardTruths": ["string"],
        "criteria": [
            {
            "criterion": "string",
            "score": number,
            "feedback": "string",
            "improvement": "string"
            }
        ],
        "finalVerdict": "Hire | Shortlist | Reject"
        }

        The "hardTruths" array should contain 1–3 honest but reasonable points about the biggest issues in the resume that would hurt the user’s chances if not fixed.

        The "finalVerdict" should be one of: "Hire", "Shortlist", or "Reject", based on realistic hiring standards.

        Your feedback should:

        - Feel like a real resume coach, not a yes‑man.
        - Gently highlight gaps and then immediately show how to fix them.
        - Make the user feel that improving those areas will clearly increase their score and chances.

        Now analyze the following resume:

        {{RESUME}}
        """;

        return template.replace("{{RESUME}}", resumeText);
    }

    // ===============================
    // 2️⃣ MOCK INTERVIEW – START (SETS THE BAR)
    // ===============================
    public String mockInterviewStartPrompt(String resumeText, String role) {
        String template = """
        You are a Principal Engineer at a top-tier tech company (Google/Amazon/Netflix). 
        You are conducting a "Bar Raiser" interview. Your job is to determine if the candidate knows their stuff deeply or is just reciting definitions.
        
        Candidate Role: {{ROLE}}
        
        Resume Context:
        {{RESUME}}
        
        INSTRUCTIONS:
        1. Identify the most impressive claim on their resume.
        2. Formulate a technical question that tests the DEPTH of that specific claim.
        3. Do NOT start with "Tell me about yourself".
        4. The question must be realistic but challenging.
        
        Output: Just the question.
        """;
        
        return template
                .replace("{{ROLE}}", role)
                .replace("{{RESUME}}", resumeText);
    }

    // ===============================
    // 3️⃣ MOCK INTERVIEW – FOLLOW-UP (ADAPTIVE DIFFICULTY ENGINE)
    // ===============================
    public String mockInterviewFollowUpPrompt(String previousQuestion, String userAnswer) {
        String template = """
        You are a Principal Engineer conducting a technical interview.
        
        Previous Question: "{{QUESTION}}"
        Candidate's Answer: "{{ANSWER}}"
        
        =========================================
        ⚠️ ADAPTIVE DIFFICULTY LOGIC (EXECUTE THIS STRICTLY) ⚠️
        
        Analyze the candidate's answer quality:
        
        1. IF THE ANSWER WAS WEAK / WRONG:
           - Action: STOP. Point out the specific error.
           - Next Step: Downgrade difficulty. Ask a foundational/basic question to check if they understand the core concepts at all.
           
        2. IF THE ANSWER WAS AVERAGE / TEXTBOOK:
           - Action: Acknowledge it briefly.
           - Next Step: Maintain difficulty. Ask a practical follow-up regarding implementation details or trade-offs.
           
        3. IF THE ANSWER WAS EXCELLENT / PERFECT:
           - Action: Challenge them. The interview is too easy.
           - Next Step: SPIKE DIFFICULTY. Ask a complex "System Design" or "Edge Case" question related to the topic. Force them to think about scaling, security, or memory optimization.
        
        =========================================
        
        OUTPUT FORMAT:
        [Feedback]: (Brutally honest feedback on previous answer, max 2 sentences)
        [Difficulty Adjustment]: (Increased / Maintained / Decreased)
        [Next Question]: (The new question based on logic above)
        """;
        
        return template
                .replace("{{QUESTION}}", previousQuestion)
                .replace("{{ANSWER}}", userAnswer);
    }

   
    // ===============================
    // CAREER PLANNER (JSON STRICT)
    // ===============================
    public String careerPlannerPrompt(String resumeText) {
        return """
        RETURN ONLY VALID JSON. NO TEXT. NO MARKDOWN.

        STRICT RULES:
        - paths MUST contain at least 1 item
        - each path MUST contain at least 5 modules  
        - modules MUST be detailed
        - description MUST be at least 15 words

        ANALYSIS PROCESS (MANDATORY - FOLLOW EXACTLY):

        1. FIRST: Extract primary career domain(s) FROM RESUME ONLY (job titles, responsibilities, projects, education, certifications, achievements). Do NOT assume software/tech.

        2. IDENTIFY career stage: student, fresher, early professional (0-2 yrs), mid-level (3-5 yrs), experienced (5+ yrs), or career switcher.

        3. DETECT skill gaps by comparing resume evidence against industry standards for detected domain(s).

        4. For NON-TECHNICAL resumes (business, marketing, HR, finance, design, healthcare, operations, education), STAY IN THAT DOMAIN. Never force tech roles.

        5. If MULTIPLE domains exist, create separate career paths for each viable track.

        6. Base ALL role suggestions on CONCRETE resume evidence (actual skills demonstrated, projects completed, responsibilities handled).

        7. Respect real-world constraints: education level, experience duration, current role level.

        8. For career transitions, suggest realistic bridge roles, not senior positions.

        9. NEVER echo or summarize resume content. Synthesize forward-looking guidance only.

        EXACT JSON FORMAT (DO NOT CHANGE):
        {
        "id": "career_path_1", 
        "skillName": "[RESUME-DRIVEN DOMAIN]",
        "suggestedRoles": ["role1", "role2", "role3"],
        "paths": [
            {
            "roleName": "Specific Role", 
            "weeks": 12,
            "modules": [
                {
                "name": "Module Name",
                "description": "Detailed description at least 15 words explaining what they'll learn and why it matters for this role.",
                "completed": false
                }
            ]
            }
        ]
        }

        Resume:
        {{RESUME}}

        """.replace("{{RESUME}}", resumeText);
    }




    // ===============================
    // 5️⃣ WRITING ASSISTANT (PROFESSIONAL POLISH)
    // ===============================
    public String writingAssistantPrompt(String task, String content) {
        
        String prompt = "";

        switch (task.toLowerCase()) {
            case "improve" -> prompt = """
                You are a ruthless Editor. The text below is likely weak, wordy, or passive.
                Rewrite it to be:
                1. Concise (remove fluff).
                2. Authoritative (active voice).
                3. Professional (corporate ready).
                
                Do not change the core facts, but change the tone to sound like a top-performer.
                
                Text:
                {{CONTENT}}
                """;

            case "email" -> prompt = """
                Write a professional business email based on the context below.
                The tone should be: Direct, respectful, and clear.
                Avoid "I hope this email finds you well" clichés. Get to the point immediately.
                
                Context:
                {{CONTENT}}
                """;

            case "linkedin" -> prompt = """
                Write a high-engagement LinkedIn post.
                The goal is: Thought Leadership.
                Structure: Strong hook -> Value/Insight -> Call to Action.
                Avoid being "cringe" or overly promotional. Use clean spacing.
                
                Topic:
                {{CONTENT}}
                """;

            case "cover_letter" -> prompt = """
                Write a cover letter that actually gets read.
                Standard cover letters are boring. Write one that hooks the recruiter in the first sentence.
                Focus on: "Here is what I can do for you," not "Here is what I want."
                
                Details:
                {{CONTENT}}
                """;

            default -> throw new IllegalArgumentException("Invalid writing task: " + task);
        }

        return prompt.replace("{{CONTENT}}", content);
    }
}