package com.ai.career.backend.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    // ===============================
    // 1️⃣ RESUME ANALYZER (NO-NONSENSE REALITY CHECK)
    // ===============================
    public String resumeAnalysisPrompt(String resumeText) {
        String template = """
        You are a cynical, elite Technical Recruiter who has reviewed 10,000+ resumes. You are allergic to fluff, buzzwords, and vague statements. 
        
        YOUR GOAL: 
        Give the user a "Reality Check". Do not be a "Yes Man". If the resume is bad, tell them exactly why it will get rejected in 6 seconds. If it is good, tell them how to make it great.
        
        INSTRUCTIONS:
        1. Ignore polite formatting. Look at the raw content.
        2. If a bullet point lacks numbers/metrics, flag it as "Weak".
        3. If the summary is generic (e.g., "Passionate developer"), roast it.
        4. Provide specific, actionable rewrites for the worst parts.

        Analyze this resume against these 9 criteria:
        1. Contact Info (Professionalism)
        2. Summary (ROI-focused vs Generic)
        3. Experience (Metrics & Impact vs Just "Tasks")
        4. Progression (Growth story)
        5. Skills (ATS Keywords relevance)
        6. Education
        7. Achievements (Quantifiable data)
        8. Formatting (Readability)
        9. ATS Compatibility

        Output ONLY in JSON:
        {
          "overallScore": number (0-100),
          "hardTruths": ["Critical feedback 1", "Critical feedback 2"],
          "criteria": [
            {
              "criterion": "1. Contact Information",
              "score": number (0-10),
              "feedback": "Brutally honest detailed feedback",
              "improvement": "Exact example of how to fix it"
            }
            // Repeat for all 9
          ],
          "finalVerdict": "Hire / Shortlist / Reject"
        }

        Resume:
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
    // 4️⃣ CAREER PLANNER (MARKET REALITY)
    // ===============================
    public String careerPlannerPrompt(String resumeText) {
        String template = """
        You are a high-end Career Strategist. You deal with market data, salaries, and hiring trends. You do not deal in "hopes and dreams".
        
        Resume:
        {{RESUME}}
        
        Your Task: Create a detailed, ruthless career roadmap.
        
        1. MARKET VALUATION: Based on this resume, what is the candidate's ACTUAL level? (Junior, Mid, Senior). Do not inflate it.
        2. THE GAPS: List the specific technical skills and soft skills missing that prevent them from getting a 30 percent pay raise.
        3. TARGET ROLES: List 3 job titles they can realistically get NOW, and 3 they should aim for in 2 years.
        4. 6-MONTH ACTION PLAN: A detailed, week-by-week study and project plan to close the gaps.
        
        Be specific. Don't say "Learn Cloud". Say "Get AWS Solutions Architect Associate certification".
        """;
        
        return template.replace("{{RESUME}}", resumeText);
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