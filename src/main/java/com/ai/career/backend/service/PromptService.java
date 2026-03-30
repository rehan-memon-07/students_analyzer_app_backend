package com.ai.career.backend.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    // ===============================
    // 1️⃣ RESUME ANALYZER
    // ===============================
    public String resumeAnalysisPrompt(String resumeText) {
        String template = """
        You are a senior resume analyst working for a modern career-guidance platform. Your job is to review resumes and help students improve them step by step. You are honest, constructive, and focused on real-world hiring expectations, but you are not harsh or cynical.

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

        Your tone should be honest and realistic, but encouraging and supportive.

        For each criterion, give:
        - A clear score (0–100).
        - Brief, specific feedback that explains what is good and what is missing.
        - Concrete, actionable improvement suggestions.

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

        The "hardTruths" array should contain 1–3 honest but reasonable points about the biggest issues.
        The "finalVerdict" should be one of: "Hire", "Shortlist", or "Reject".

        ⚠️ SCORE ENFORCEMENT (NON-NEGOTIABLE):
        - overallScore 0-40 → finalVerdict: "Reject"
        - overallScore 41-69 → finalVerdict: "Shortlist"
        - overallScore 70-100 → finalVerdict: "Hire"
        Never deviate from these ranges.

        Now analyze the following resume:
        {{RESUME}}
        """;

        return template.replace("{{RESUME}}", resumeText);
    }

    // ===============================
    // RESUME VERSION COMPARISON
    // ===============================
    public String resumeComparisonPrompt(String resumeTextA, String resumeTextB) {
        String template = """
        You are a senior technical recruiter and resume expert at a top-tier company.
        You are comparing two versions of a candidate's resume to determine which is stronger and why.

        VERSION A (Older):
        {{RESUME_A}}

        =========================================

        VERSION B (Newer):
        {{RESUME_B}}

        =========================================

        INSTRUCTIONS:
        - Evaluate both versions across the same criteria.
        - Be brutally specific. Do not give vague feedback.
        - Identify exact improvements, regressions, and unchanged areas.
        - Give a final recommendation on which version to use.

        OUTPUT ONLY IN JSON (no markdown, no explanation outside JSON):

        {
          "overallVerdict": "string (1-2 sentence summary of which is better and why)",
          "recommendation": "Version A" | "Version B" | "Both are Equal",
          "scoreDelta": number (positive = B is better, negative = A is better, 0 = equal),
          "scoreA": number (0-100),
          "scoreB": number (0-100),
          "categories": [
            {
              "name": "string",
              "scoreA": number (0-10),
              "scoreB": number (0-10),
              "change": "improved" | "regressed" | "unchanged",
              "insight": "string"
            }
          ],
          "improvements": ["string"],
          "regressions": ["string"],
          "unchanged": ["string"],
          "finalAdvice": "string"
        }
        """;

        return template
                .replace("{{RESUME_A}}", resumeTextA)
                .replace("{{RESUME_B}}", resumeTextB);
    }

    // ===============================
    // 2️⃣ MOCK INTERVIEW – START
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

        Output: Just the question. Nothing else.
        """;

        return template
                .replace("{{ROLE}}", role)
                .replace("{{RESUME}}", resumeText);
    }

    // ===============================
    // 3️⃣ MOCK INTERVIEW – FOLLOW-UP (ADAPTIVE + AI DETECTION)
    // ===============================
    public String mockInterviewFollowUpPrompt(String previousQuestion, String userAnswer, int warningCount) {
        String template = """
        You are a Principal Engineer conducting a technical interview.

        Previous Question: "{{QUESTION}}"
        Candidate's Answer: "{{ANSWER}}"
        Previous AI-Detection Warnings Issued: {{WARNING_COUNT}}

        =========================================
        STEP 1: AI-GENERATED ANSWER DETECTION

        Carefully check if the answer appears to be AI-generated or copied from an AI tool.
        Signs of AI-generated answers:
        - Unnaturally perfect structure with bullet points and numbered lists
        - Generic, textbook-perfect examples with no personal experience
        - Phrases like "certainly", "absolutely", "it's worth noting", "in conclusion"
        - Overly comprehensive coverage of every edge case
        - No personal pronouns or real-world anecdotes
        - Suspiciously polished grammar for a live interview setting

        Set [AI Detected] to true if 3+ signs are present. Be fair — a well-prepared candidate can give structured answers.

        =========================================
        STEP 2: ADAPTIVE DIFFICULTY LOGIC

        Analyze the candidate's answer quality:

        1. IF THE ANSWER WAS WEAK / WRONG:
           - Point out the specific error.
           - Downgrade difficulty. Ask a foundational/basic question.

        2. IF THE ANSWER WAS AVERAGE / TEXTBOOK:
           - Acknowledge it briefly.
           - Maintain difficulty. Ask a practical follow-up.

        3. IF THE ANSWER WAS EXCELLENT:
           - Challenge them. SPIKE DIFFICULTY.
           - Ask a System Design or Edge Case question.

        IF AI DETECTED AND WARNING_COUNT >= 2:
           - Note the violation in feedback.
           - Ask a highly specific, personal question that an AI cannot answer (e.g., "Walk me through a specific bug you personally debugged in your last project")

        =========================================

        OUTPUT FORMAT (use EXACTLY these labels, each on its own line):
        [Feedback]: (Honest feedback on previous answer, max 2 sentences. If AI detected, note it here.)
        [Difficulty Adjustment]: (Increased / Maintained / Decreased)
        [AI Detected]: (true / false)
        [Next Question]: (The new question)
        """;

        return template
                .replace("{{QUESTION}}", previousQuestion)
                .replace("{{ANSWER}}", userAnswer)
                .replace("{{WARNING_COUNT}}", String.valueOf(warningCount));
    }

    // ===============================
    // 4️⃣ MOCK INTERVIEW – END (FINAL EVALUATION)
    // ===============================
    public String mockInterviewEndPrompt(
            java.util.List<com.ai.career.backend.dto.MockInterviewEndRequest.QA> conversation,
            String role,
            int totalAiWarnings
    ) {
        StringBuilder conversationBuilder = new StringBuilder();

        for (com.ai.career.backend.dto.MockInterviewEndRequest.QA qa : conversation) {
            conversationBuilder.append("Q: ").append(qa.getQuestion()).append("\n");
            conversationBuilder.append("A: ").append(qa.getAnswer()).append("\n\n");
        }

        String template = """
        You are a Principal Engineer and Hiring Committee Member at a top-tier tech company.
        You just completed a technical interview.

        Candidate Role: {{ROLE}}
        AI-Generated Answer Violations During Interview: {{AI_WARNINGS}}

        Your task: Provide a FINAL hiring evaluation.
        Be brutally honest. Do not sugarcoat.
        If the candidate is weak, clearly say they would be rejected.
        If strong, justify why they pass.

        Evaluate on:
        1. Technical Depth
        2. Clarity of Communication
        3. Problem-Solving Ability
        4. Confidence & Ownership
        5. Real-World Readiness

        NOTE ON AI VIOLATIONS:
        - If AI_WARNINGS >= 3: Deduct 10-15 points from overallScore for academic dishonesty.
        - If AI_WARNINGS == 2: Note it in weaknesses but do not heavily penalize.
        - If AI_WARNINGS <= 1: Ignore for scoring purposes.

        =========================================

        OUTPUT ONLY IN JSON (no markdown, no text outside JSON):

        {
          "overallScore": number (0-100),
          "technicalDepthScore": number (0-10),
          "communicationScore": number (0-10),
          "problemSolvingScore": number (0-10),
          "confidenceScore": number (0-10),
          "strengths": ["Point 1", "Point 2"],
          "weaknesses": ["Point 1", "Point 2"],
          "hireDecision": "Strong Hire / Hire / Lean Hire / Lean Reject / Reject",
          "finalVerdict": "Brutally honest summary paragraph"
        }

        =========================================

        ⚠️ SCORE-TO-HIRE ENFORCEMENT (NON-NEGOTIABLE — NEVER DEVIATE):
        - overallScore 0-40   → hireDecision MUST be "Reject"
        - overallScore 41-55  → hireDecision MUST be "Lean Reject"
        - overallScore 56-69  → hireDecision MUST be "Lean Hire"
        - overallScore 70-84  → hireDecision MUST be "Hire"
        - overallScore 85-100 → hireDecision MUST be "Strong Hire"
        These are absolute rules. Never give "Hire" to a score below 70.
        Never give "Strong Hire" to a score below 85.

        =========================================

        Interview Conversation:
        {{CONVERSATION}}
        """;

        return template
                .replace("{{ROLE}}", role)
                .replace("{{AI_WARNINGS}}", String.valueOf(totalAiWarnings))
                .replace("{{CONVERSATION}}", conversationBuilder.toString());
    }

    // ===============================
    // 5️⃣ CAREER PLANNER (COMING SOON — DO NOT MODIFY)
    // ===============================
    public String careerPlannerPrompt(String resumeText) {
        return """
        RETURN ONLY VALID JSON. NO TEXT. NO MARKDOWN.

        STRICT RULES:
        - paths MUST contain at least 1 item
        - each path MUST contain at least 5 modules
        - modules MUST be detailed
        - description MUST be at least 15 words

        ANALYSIS PROCESS (MANDATORY):
        1. Extract primary career domain FROM RESUME ONLY.
        2. Identify career stage.
        3. Detect skill gaps.
        4. For NON-TECHNICAL resumes, STAY IN THAT DOMAIN.
        5. If MULTIPLE domains exist, create separate career paths.
        6. Base ALL role suggestions on CONCRETE resume evidence.
        7. Respect real-world constraints.
        8. For career transitions, suggest realistic bridge roles.
        9. NEVER echo resume content. Synthesize forward-looking guidance only.

        EXACT JSON FORMAT:
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
                  "description": "Detailed description at least 15 words explaining what they'll learn and why it matters.",
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
    // 6️⃣ WRITING ASSISTANT
    // ===============================
    public String writingAssistantPrompt(String task, String content) {
        String prompt;

        switch (task.toLowerCase()) {
            case "improve" -> prompt = """
                You are a ruthless Editor. Rewrite the text to be:
                1. Concise (remove fluff).
                2. Authoritative (active voice).
                3. Professional (corporate ready).
                Do not change the core facts, but change the tone to sound like a top-performer.
                Text: {{CONTENT}}
                """;

            case "email" -> prompt = """
                Write a professional business email based on the context below.
                Tone: Direct, respectful, and clear.
                Avoid "I hope this email finds you well" clichés. Get to the point immediately.
                Context: {{CONTENT}}
                """;

            case "linkedin" -> prompt = """
                Write a high-engagement LinkedIn post.
                Goal: Thought Leadership.
                Structure: Strong hook -> Value/Insight -> Call to Action.
                Avoid being cringe or overly promotional. Use clean spacing.
                Topic: {{CONTENT}}
                """;

            case "cover_letter" -> prompt = """
                Write a cover letter that actually gets read.
                Hook the recruiter in the first sentence.
                Focus on: "Here is what I can do for you," not "Here is what I want."
                Details: {{CONTENT}}
                """;

            default -> throw new IllegalArgumentException("Invalid writing task: " + task);
        }

        return prompt.replace("{{CONTENT}}", content);
    }
}