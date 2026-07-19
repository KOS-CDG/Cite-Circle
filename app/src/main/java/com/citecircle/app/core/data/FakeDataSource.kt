package com.citecircle.app.core.data

import com.citecircle.app.core.model.*

/**
 * In-memory fake data source with ~30 realistic users, ~50 posts, ~15 papers,
 * ~8 circles, comments, notifications, and conversations.
 * Structure allows easy swap-out for a real API layer.
 */
object FakeDataSource {

    // ──────────────────────────────────────────────────────────────────────────
    // USERS (30 realistic academics)
    // ──────────────────────────────────────────────────────────────────────────

    val defaultUser = User(
        id = "u0",
        name = "Maya Okafor",
        avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=maya",
        role = UserRole.RESEARCHER,
        institution = "MIT Media Lab",
        fieldOfStudy = "Human-Computer Interaction",
        bio = "PhD candidate exploring situated cognition in AI-augmented workspaces. Ex-Google Brain. She/her.",
        orcidId = "0000-0002-7834-1291",
        followerCount = 847,
        followingCount = 312,
        citationCount = 1240,
        isVerified = true,
        interests = listOf("HCI", "AI", "Cognitive Science", "Design")
    )

    val superAdminUser = User(
        id = "admin",
        name = "Super Admin",
        avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=admin",
        role = UserRole.ADMIN,
        institution = "CiteCircle Operations",
        fieldOfStudy = "System Administration",
        bio = "Super Admin account with full platform capabilities and settings access.",
        orcidId = "0000-0000-0000-0000",
        followerCount = 9999,
        followingCount = 0,
        citationCount = 0,
        isVerified = true,
        interests = listOf("Security", "Moderation", "Infrastructure")
    )

    val dummyUser = User(
        id = "dummy",
        name = "Dummy Scholar",
        avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=dummy",
        role = UserRole.STUDENT,
        institution = "CiteCircle Academy",
        fieldOfStudy = "General Studies",
        bio = "A dummy account for checking the student interface and frontend layout.",
        orcidId = "0000-1111-2222-3333",
        followerCount = 10,
        followingCount = 20,
        citationCount = 0,
        isVerified = false,
        interests = listOf("HCI", "Machine Learning", "NLP")
    )

    val aiUser = User(
        id = "ai_copilot",
        name = "CiteCircle AI Copilot",
        avatarUrl = "https://api.dicebear.com/8.x/bottts/svg?seed=copilot",
        role = UserRole.RESEARCHER,
        institution = "CiteCircle AI Lab",
        fieldOfStudy = "Academic Research Assistant",
        bio = "CiteCircle's research assistant. Ask me anything about literature reviews, methodology, or academic writing.",
        orcidId = "0000-0000-1111-9999",
        followerCount = 10000,
        followingCount = 0,
        citationCount = 0,
        isVerified = true,
        interests = listOf("AI", "Research", "Education", "Methodology")
    )

    var currentUser = defaultUser

    val users = listOf(
        User(
            id = "u1",
            name = "Dr. Elena Reyes",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=elena",
            role = UserRole.EDUCATOR,
            institution = "Stanford University",
            fieldOfStudy = "Computational Biology",
            bio = "Associate Professor of Computational Biology. PI of the Reyes Lab. NSF CAREER Award 2021.",
            orcidId = "0000-0001-5523-4521",
            followerCount = 4320,
            followingCount = 210,
            citationCount = 8740,
            isVerified = true,
            isFollowing = true,
            interests = listOf("Genomics", "Machine Learning", "Proteomics")
        ),
        User(
            id = "u2",
            name = "Prof. James Whitmore",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=james",
            role = UserRole.EDUCATOR,
            institution = "Harvard University",
            fieldOfStudy = "Cognitive Neuroscience",
            bio = "William James Professor of Cognitive Science. Bestselling author of 'The Attention Economy of the Mind'.",
            orcidId = "0000-0003-1234-5678",
            followerCount = 12890,
            followingCount = 89,
            citationCount = 34200,
            isVerified = true,
            interests = listOf("Neuroscience", "Psychology", "Decision Making")
        ),
        User(
            id = "u3",
            name = "Aisha Nakamura",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=aisha",
            role = UserRole.RESEARCHER,
            institution = "University of Tokyo",
            fieldOfStudy = "Quantum Computing",
            bio = "Postdoctoral researcher in quantum error correction. JST Fellowship. Interested in fault-tolerant computation.",
            orcidId = "0000-0002-9981-3345",
            followerCount = 2100,
            followingCount = 450,
            citationCount = 3100,
            isVerified = true,
            isConnected = true,
            interests = listOf("Quantum Computing", "Physics", "Information Theory")
        ),
        User(
            id = "u4",
            name = "Carlos Mendoza",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=carlos",
            role = UserRole.STUDENT,
            institution = "UC Berkeley",
            fieldOfStudy = "Machine Learning",
            bio = "3rd-year PhD student in CS, advised by Prof. Jordan. Working on causal inference in large language models.",
            orcidId = "",
            followerCount = 680,
            followingCount = 920,
            citationCount = 245,
            isVerified = false,
            isFollowing = true,
            interests = listOf("Machine Learning", "Causality", "NLP")
        ),
        User(
            id = "u5",
            name = "Dr. Sophie Laurent",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=sophie",
            role = UserRole.RESEARCHER,
            institution = "CNRS Paris",
            fieldOfStudy = "Sociology of Science",
            bio = "CNRS researcher studying how academic publishing shapes scientific progress. @bibliometrics @openscience",
            orcidId = "0000-0001-8842-0011",
            followerCount = 3200,
            followingCount = 700,
            citationCount = 5600,
            isVerified = true,
            interests = listOf("Sociology", "Open Science", "Bibliometrics")
        ),
        User(
            id = "u6",
            name = "Kwame Asante",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=kwame",
            role = UserRole.STUDENT,
            institution = "University of Ghana",
            fieldOfStudy = "Educational Technology",
            bio = "MSc student exploring mobile-first pedagogy in sub-Saharan Africa. Google Generation Scholar 2024.",
            orcidId = "",
            followerCount = 340,
            followingCount = 520,
            citationCount = 12,
            isVerified = false,
            isConnected = true,
            interests = listOf("Education", "Technology", "Africa", "Mobile Learning")
        ),
        User(
            id = "u7",
            name = "Prof. Mei-Lin Chen",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=meilin",
            role = UserRole.EDUCATOR,
            institution = "Peking University",
            fieldOfStudy = "Environmental Science",
            bio = "Chair, Department of Environmental Sciences. Lead author IPCC AR6. Focus on urban carbon capture.",
            orcidId = "0000-0003-7701-9982",
            followerCount = 9800,
            followingCount = 155,
            citationCount = 22100,
            isVerified = true,
            interests = listOf("Climate Science", "Urban Ecology", "Carbon")
        ),
        User(
            id = "u8",
            name = "Tariq Al-Rashid",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=tariq",
            role = UserRole.RESEARCHER,
            institution = "King Abdullah University",
            fieldOfStudy = "Materials Science",
            bio = "Materials engineer specializing in perovskite solar cells. Collaboration across 4 continents.",
            orcidId = "0000-0002-3345-8821",
            followerCount = 1890,
            followingCount = 380,
            citationCount = 2870,
            isVerified = true,
            interests = listOf("Materials Science", "Solar Energy", "Nanomaterials")
        ),
        User(
            id = "u9",
            name = "Priya Venkataraman",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=priya",
            role = UserRole.STUDENT,
            institution = "IIT Bombay",
            fieldOfStudy = "Bioinformatics",
            bio = "Final-year undergrad in CS/Biology. Passionate about protein structure prediction. Future PhD applicant.",
            orcidId = "",
            followerCount = 190,
            followingCount = 460,
            citationCount = 0,
            isVerified = false,
            interests = listOf("Bioinformatics", "Structural Biology", "AI")
        ),
        User(
            id = "u10",
            name = "Dr. Noah Bergman",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=noah",
            role = UserRole.EDUCATOR,
            institution = "ETH Zürich",
            fieldOfStudy = "Applied Mathematics",
            bio = "Assistant Professor of Applied Math. Working on numerical methods for PDEs in fluid dynamics.",
            orcidId = "0000-0001-4499-2211",
            followerCount = 2450,
            followingCount = 300,
            citationCount = 4100,
            isVerified = true,
            isFollowing = true,
            interests = listOf("Mathematics", "Fluid Dynamics", "Numerical Analysis")
        ),
        User(
            id = "u11",
            name = "Fatima Al-Zahrawi",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=fatima",
            role = UserRole.RESEARCHER,
            institution = "WHO Geneva",
            fieldOfStudy = "Epidemiology",
            bio = "Global health researcher. COVID-19 surveillance lead. Advocate for equitable vaccine distribution.",
            orcidId = "0000-0002-6610-4453",
            followerCount = 7600,
            followingCount = 230,
            citationCount = 15800,
            isVerified = true,
            isConnected = true,
            interests = listOf("Epidemiology", "Global Health", "Vaccines")
        ),
        User(
            id = "u12",
            name = "Lena Hoffmann",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=lena",
            role = UserRole.STUDENT,
            institution = "Humboldt University",
            fieldOfStudy = "Digital Humanities",
            bio = "PhD candidate in Digital Humanities. Applying NLP to 19th-century German correspondence networks.",
            orcidId = "",
            followerCount = 450,
            followingCount = 620,
            citationCount = 35,
            isVerified = false,
            interests = listOf("Digital Humanities", "NLP", "History", "Network Analysis")
        ),
        User(
            id = "u13",
            name = "Prof. Samuel Adeyemi",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=samuel",
            role = UserRole.EDUCATOR,
            institution = "University of Lagos",
            fieldOfStudy = "Economics",
            bio = "Development economist. Research on fintech's impact on financial inclusion in West Africa. @WorldBank consultant.",
            orcidId = "0000-0001-9983-2120",
            followerCount = 5100,
            followingCount = 175,
            citationCount = 9300,
            isVerified = true,
            interests = listOf("Economics", "Fintech", "Development", "Africa")
        ),
        User(
            id = "u14",
            name = "Yuki Tanaka",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=yuki",
            role = UserRole.RESEARCHER,
            institution = "RIKEN",
            fieldOfStudy = "Theoretical Physics",
            bio = "RIKEN iTHEMS fellow. Studying topological phases of matter and anyonic statistics.",
            orcidId = "0000-0003-2211-6699",
            followerCount = 1350,
            followingCount = 290,
            citationCount = 2100,
            isVerified = true,
            interests = listOf("Physics", "Topology", "Condensed Matter")
        ),
        User(
            id = "u15",
            name = "Isabella Ferreira",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=isabella",
            role = UserRole.STUDENT,
            institution = "USP São Paulo",
            fieldOfStudy = "Cognitive Psychology",
            bio = "MSc in Cognitive Psych. Studying cross-cultural differences in moral reasoning using computational methods.",
            orcidId = "",
            followerCount = 280,
            followingCount = 410,
            citationCount = 8,
            isVerified = false,
            interests = listOf("Psychology", "Moral Cognition", "Cross-Cultural")
        ),
        User(
            id = "u16",
            name = "Dr. Rajesh Patel",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=rajesh",
            role = UserRole.EDUCATOR,
            institution = "IISc Bangalore",
            fieldOfStudy = "Robotics & AI",
            bio = "Professor of Robotics. Director of the Autonomous Systems Lab. TEDx speaker on ethical AI.",
            orcidId = "0000-0002-4411-7823",
            followerCount = 8200,
            followingCount = 320,
            citationCount = 18400,
            isVerified = true,
            interests = listOf("Robotics", "AI Ethics", "Autonomous Systems")
        ),
        User(
            id = "u17",
            name = "Amara Osei",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=amara",
            role = UserRole.STUDENT,
            institution = "Oxford University",
            fieldOfStudy = "Global History",
            bio = "DPhil candidate. Thesis on colonial botanical knowledge networks 1750–1900. Rhodes Scholar.",
            orcidId = "",
            followerCount = 560,
            followingCount = 380,
            citationCount = 0,
            isVerified = false,
            interests = listOf("History", "Botany", "Colonial Studies", "Network History")
        ),
        User(
            id = "u18",
            name = "Prof. Dimitri Kostopoulos",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=dimitri",
            role = UserRole.EDUCATOR,
            institution = "University of Athens",
            fieldOfStudy = "Marine Biology",
            bio = "Professor of Marine Ecology. Mediterranean research network coordinator. Coral reef restoration advocate.",
            orcidId = "0000-0001-7763-4451",
            followerCount = 3700,
            followingCount = 260,
            citationCount = 7200,
            isVerified = true,
            interests = listOf("Marine Biology", "Ecology", "Mediterranean", "Coral")
        ),
        User(
            id = "u19",
            name = "Sunita Krishnaswamy",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=sunita",
            role = UserRole.RESEARCHER,
            institution = "Broad Institute",
            fieldOfStudy = "Genomics",
            bio = "Staff scientist at the Broad. GWAS, polygenic risk scores, and population genomics. Open data advocate.",
            orcidId = "0000-0002-8812-3322",
            followerCount = 4500,
            followingCount = 390,
            citationCount = 9800,
            isVerified = true,
            isFollowing = true,
            interests = listOf("Genomics", "GWAS", "Population Genetics")
        ),
        User(
            id = "u20",
            name = "Ben Cartwright",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=ben",
            role = UserRole.STUDENT,
            institution = "Cambridge University",
            fieldOfStudy = "Computer Science",
            bio = "Part III student in CS. Research internship at DeepMind this summer. Building interpretable ML tools.",
            orcidId = "",
            followerCount = 420,
            followingCount = 580,
            citationCount = 18,
            isVerified = false,
            isConnected = true,
            interests = listOf("ML Interpretability", "CS Theory", "AI Safety")
        ),
        User(
            id = "u21",
            name = "Dr. Valeria Moreno",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=valeria",
            role = UserRole.RESEARCHER,
            institution = "Max Planck Institute",
            fieldOfStudy = "Evolutionary Biology",
            bio = "Max Planck Fellow. Studying convergent evolution using ancient DNA. Collaborator on the 1000 Genomes Project.",
            orcidId = "0000-0003-5501-8934",
            followerCount = 2900,
            followingCount = 410,
            citationCount = 5100,
            isVerified = true,
            interests = listOf("Evolutionary Biology", "Ancient DNA", "Genomics")
        ),
        User(
            id = "u22",
            name = "Oliver Ndungu",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=oliver",
            role = UserRole.STUDENT,
            institution = "University of Nairobi",
            fieldOfStudy = "Public Health",
            bio = "MPH student. Research on urban malaria transmission and community health worker effectiveness.",
            orcidId = "",
            followerCount = 210,
            followingCount = 340,
            citationCount = 0,
            isVerified = false,
            interests = listOf("Public Health", "Malaria", "Community Health")
        ),
        User(
            id = "u23",
            name = "Prof. Hiroshi Yamamoto",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=hiroshi",
            role = UserRole.EDUCATOR,
            institution = "Kyoto University",
            fieldOfStudy = "Artificial Intelligence",
            bio = "Turing Award honorable mention. Research on symbolic reasoning and neural-symbolic integration.",
            orcidId = "0000-0001-3321-7765",
            followerCount = 15600,
            followingCount = 72,
            citationCount = 45000,
            isVerified = true,
            interests = listOf("AI", "Symbolic Reasoning", "Knowledge Representation")
        ),
        User(
            id = "u24",
            name = "Chiara Bianchi",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=chiara",
            role = UserRole.RESEARCHER,
            institution = "University of Bologna",
            fieldOfStudy = "Medieval History",
            bio = "Marie Curie Fellow. Using network analysis to map scholarly communication in 14th-century Italy.",
            orcidId = "0000-0002-1198-4432",
            followerCount = 1100,
            followingCount = 320,
            citationCount = 870,
            isVerified = false,
            interests = listOf("Medieval History", "Network Analysis", "Digital Methods")
        ),
        User(
            id = "u25",
            name = "Dr. Ahmed Hassan",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=ahmed",
            role = UserRole.EDUCATOR,
            institution = "Cairo University",
            fieldOfStudy = "Electrical Engineering",
            bio = "Associate Professor of EE. Focus on next-gen wireless communication: 6G, MIMO, beamforming.",
            orcidId = "0000-0001-6612-9988",
            followerCount = 3100,
            followingCount = 280,
            citationCount = 6400,
            isVerified = true,
            interests = listOf("Wireless Communication", "6G", "Signal Processing")
        ),
        User(
            id = "u26",
            name = "Mia Sorensen",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=mia",
            role = UserRole.STUDENT,
            institution = "DTU Copenhagen",
            fieldOfStudy = "Sustainable Engineering",
            bio = "Undergrad in Sustainable Engineering. Semester project on wave energy converter optimization.",
            orcidId = "",
            followerCount = 145,
            followingCount = 290,
            citationCount = 0,
            isVerified = false,
            interests = listOf("Renewable Energy", "Ocean Engineering", "Sustainability")
        ),
        User(
            id = "u27",
            name = "Prof. Anya Volkov",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=anya",
            role = UserRole.EDUCATOR,
            institution = "Moscow State University",
            fieldOfStudy = "Mathematical Physics",
            bio = "Professor of Mathematical Physics. Expert in integrable systems and soliton theory.",
            orcidId = "0000-0003-4482-1119",
            followerCount = 4100,
            followingCount = 190,
            citationCount = 11200,
            isVerified = true,
            interests = listOf("Mathematical Physics", "Solitons", "Integrable Systems")
        ),
        User(
            id = "u28",
            name = "Jae-won Park",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=jaewon",
            role = UserRole.RESEARCHER,
            institution = "KAIST",
            fieldOfStudy = "NLP",
            bio = "Postdoc at KAIST AI Graduate School. Research on multilingual LLMs and low-resource NLP for Asian languages.",
            orcidId = "0000-0002-7741-2256",
            followerCount = 2200,
            followingCount = 560,
            citationCount = 3800,
            isVerified = true,
            interests = listOf("NLP", "Multilingual AI", "Low-Resource Languages")
        ),
        User(
            id = "u29",
            name = "Rosa Jiménez",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=rosa",
            role = UserRole.STUDENT,
            institution = "UNAM Mexico City",
            fieldOfStudy = "Archaeology",
            bio = "PhD candidate in Mesoamerican archaeology. Using LiDAR and satellite imagery to map pre-Columbian settlements.",
            orcidId = "",
            followerCount = 380,
            followingCount = 450,
            citationCount = 4,
            isVerified = false,
            interests = listOf("Archaeology", "Remote Sensing", "Mesoamerica", "LiDAR")
        )
    )

    fun getAllUsers() = listOf(currentUser) + users
    fun getUserById(id: String) = getAllUsers().find { it.id == id }

    // ──────────────────────────────────────────────────────────────────────────
    // CIRCLES (8 communities)
    // ──────────────────────────────────────────────────────────────────────────

    val circles = listOf(
        Circle(
            id = "c1",
            name = "Machine Learning Frontiers",
            description = "Cutting-edge ML research: architectures, theory, applications, and reproducibility challenges. Where researchers share preprints and debate methodologies.",
            iconEmoji = "🤖",
            bannerColor = 0xFF3E63DD,
            memberCount = 24800,
            category = "Computer Science",
            isJoined = true,
            postCount = 8900,
            weeklyPostCount = 340,
            weeklyActivity = listOf(28, 45, 62, 38, 55, 71, 48)
        ),
        Circle(
            id = "c2",
            name = "Genomics & Precision Medicine",
            description = "From GWAS to gene therapy: a community for genomicists, bioinformaticians, and clinicians working at the intersection of data science and medicine.",
            iconEmoji = "🧬",
            bannerColor = 0xFF2AB3A6,
            memberCount = 18200,
            category = "Life Sciences",
            isJoined = true,
            postCount = 5600,
            weeklyPostCount = 218,
            weeklyActivity = listOf(20, 32, 25, 40, 18, 35, 29)
        ),
        Circle(
            id = "c3",
            name = "Cognitive & Behavioral Sciences",
            description = "Bridging experimental psychology, neuroscience, and computational modeling of the mind.",
            iconEmoji = "🧠",
            bannerColor = 0xFFFF6B6B,
            memberCount = 12400,
            category = "Social Sciences",
            isJoined = false,
            postCount = 3100,
            weeklyPostCount = 145,
            weeklyActivity = listOf(12, 18, 22, 16, 20, 14, 19)
        ),
        Circle(
            id = "c4",
            name = "Open Education Research",
            description = "Pedagogy, learning analytics, MOOCs, and the future of educational technology. All teaching levels welcome.",
            iconEmoji = "📚",
            bannerColor = 0xFFFFC53D,
            memberCount = 9700,
            category = "Education",
            isJoined = true,
            postCount = 2800,
            weeklyPostCount = 98,
            weeklyActivity = listOf(8, 15, 12, 20, 11, 16, 10)
        ),
        Circle(
            id = "c5",
            name = "Quantum Information & Computing",
            description = "Algorithms, hardware, error correction, and the road to fault-tolerant quantum computers.",
            iconEmoji = "⚛️",
            bannerColor = 0xFF8A94A6,
            memberCount = 7300,
            category = "Math & Physics",
            isJoined = false,
            postCount = 1900,
            weeklyPostCount = 72,
            weeklyActivity = listOf(6, 9, 11, 8, 14, 10, 7)
        ),
        Circle(
            id = "c6",
            name = "Digital Humanities Lab",
            description = "Where historical inquiry meets computational methods: text mining, network analysis, GIS, and data visualization for humanists.",
            iconEmoji = "🏛️",
            bannerColor = 0xFFFF6B6B,
            memberCount = 5600,
            category = "Humanities",
            isJoined = false,
            postCount = 1400,
            weeklyPostCount = 55,
            weeklyActivity = listOf(4, 7, 9, 5, 8, 6, 7)
        ),
        Circle(
            id = "c7",
            name = "Climate & Sustainability Science",
            description = "Evidence-based discussion of climate science, policy, and solutions. IPCC authors, climate modelers, and advocates welcome.",
            iconEmoji = "🌍",
            bannerColor = 0xFF2AB3A6,
            memberCount = 21000,
            category = "Life Sciences",
            isJoined = true,
            postCount = 6700,
            weeklyPostCount = 280,
            weeklyActivity = listOf(22, 38, 44, 31, 49, 36, 40)
        ),
        Circle(
            id = "c8",
            name = "NLP & Language Technologies",
            description = "Research and applications in natural language processing: LLMs, multilingual models, text generation, and evaluation benchmarks.",
            iconEmoji = "💬",
            bannerColor = 0xFF3E63DD,
            memberCount = 16500,
            category = "Computer Science",
            isJoined = false,
            postCount = 4800,
            weeklyPostCount = 195,
            weeklyActivity = listOf(16, 28, 35, 24, 42, 30, 27)
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // PAPERS (15 papers)
    // ──────────────────────────────────────────────────────────────────────────

    val papers = listOf(
        Paper(
            id = "p1",
            title = "Situated Cognition in AI-Augmented Knowledge Work: A Mixed-Methods Study of Academic Researchers",
            authors = listOf(currentUser, users[0]),
            abstract = "As AI writing and analysis tools become embedded in research workflows, questions arise about how they reshape cognitive load, epistemic agency, and collaborative knowledge creation. We conducted a 6-month longitudinal mixed-methods study with 42 academic researchers across disciplines, combining experience sampling, screen recordings, and in-depth interviews. We identify four interaction patterns—delegation, negotiation, augmentation, and resistance—and argue that AI-augmented research requires a new theoretical framework we term 'distributed epistemic scaffolding.' Implications for AI tool design and academic integrity policies are discussed.",
            fieldTags = listOf("HCI", "AI", "Cognitive Science", "Academic Writing"),
            citationCount = 48,
            year = 2024,
            doi = "10.1145/3613904.3642234",
            circleId = "c1",
            aiScore = 87,
            journal = "CHI 2024"
        ),
        Paper(
            id = "p2",
            title = "Polygenic Risk Score Calibration Across Diverse Ancestry Groups: Lessons from the PAGE Study",
            authors = listOf(users[0], users[18]),
            abstract = "Polygenic risk scores (PRS) derived predominantly from European ancestry GWAS cohorts exhibit significantly reduced predictive accuracy in non-European populations, exacerbating existing health disparities. We harmonize 14 trait-specific PRS across 51,000 individuals of African, Latino, Asian, and Native Hawaiian ancestry from the PAGE Study. Using elastic net calibration on ancestry-stratified principal components, we improve cross-ancestry PRS transferability by 23–41% without requiring additional GWAS data. We release calibrated weights and validation code openly.",
            fieldTags = listOf("Genomics", "GWAS", "Health Equity", "Statistics"),
            citationCount = 312,
            year = 2023,
            doi = "10.1038/s41588-023-01429-8",
            circleId = "c2",
            aiScore = 94,
            journal = "Nature Genetics"
        ),
        Paper(
            id = "p3",
            title = "Topological Phase Transitions in Non-Hermitian Floquet Systems with Time-Reversal Symmetry Breaking",
            authors = listOf(users[13], users[26]),
            abstract = "We present a theoretical study of topological phase transitions in non-Hermitian Floquet systems where time-reversal symmetry is explicitly broken by the periodic drive. Using the non-Hermitian Floquet-Bloch formalism, we derive a bulk-boundary correspondence generalized for the non-Hermitian skin effect. We classify topological invariants using a modified Chern number and demonstrate numerically that edge-localized modes persist across a broad parameter regime accessible to current experimental platforms in photonic crystals and ultracold atoms.",
            fieldTags = listOf("Topology", "Quantum Physics", "Condensed Matter"),
            citationCount = 67,
            year = 2024,
            doi = "10.1103/PhysRevLett.132.156801",
            circleId = "c5",
            aiScore = 91,
            journal = "Physical Review Letters"
        ),
        Paper(
            id = "p4",
            title = "Causal Disentanglement in Instruction-Tuned Language Models: Does RLHF Improve Causal Reasoning?",
            authors = listOf(users[3], users[27]),
            abstract = "Despite impressive performance on reasoning benchmarks, large language models (LLMs) remain brittle in causal inference tasks. We investigate whether reinforcement learning from human feedback (RLHF) training improves the underlying causal reasoning capabilities of LLMs or primarily teaches surface-level task compliance. Using a battery of 2,400 counterfactual probes adapted from causal inference literature, we find that RLHF-aligned models outperform their base counterparts on causal reasoning by 11–18%, but this advantage largely disappears on out-of-distribution causal structures, suggesting the improvement is partially a calibration artifact.",
            fieldTags = listOf("NLP", "Causal Inference", "LLM", "RLHF"),
            citationCount = 134,
            year = 2024,
            doi = "10.48550/arXiv.2403.18921",
            circleId = "c8",
            aiScore = 82,
            journal = "ACL 2024"
        ),
        Paper(
            id = "p5",
            title = "Mobile-First Pedagogy in Low-Bandwidth Environments: Outcomes from a Randomized Trial in Rural Ghana",
            authors = listOf(users[5], users[22]),
            abstract = "Despite high mobile phone penetration, educational technology interventions in sub-Saharan Africa often assume reliable internet access. We conducted a 9-month cluster-randomized trial across 24 rural Ghanaian primary schools (n=1,840 students) evaluating a mobile-first, offline-capable learning platform against standard instruction. The intervention group showed significant improvements in numeracy (d=0.42, p<0.001) and literacy (d=0.31, p=0.004) outcomes. Qualitative analysis reveals that teacher training quality moderates intervention effectiveness more than platform features.",
            fieldTags = listOf("EdTech", "Randomized Trial", "Mobile Learning", "Ghana"),
            citationCount = 89,
            year = 2023,
            doi = "10.1016/j.compedu.2023.104871",
            circleId = "c4",
            aiScore = 88,
            journal = "Computers & Education"
        ),
        Paper(
            id = "p6",
            title = "Convergent Evolution of Thermophily in Archaea: Phylogenomic Evidence Across 8,000 Sequenced Genomes",
            authors = listOf(users[20], users[0]),
            abstract = "Thermophily—the adaptation to high-temperature environments—has evolved independently in multiple archaeal lineages, but the genomic mechanisms underpinning this convergence remain contested. Analyzing 8,124 archaeal genomes spanning 13 phyla, we identify 47 genomic signatures of thermophilic adaptation including GC content biases, codon usage shifts, and a conserved thermostability module in the chaperone network. Maximum likelihood ancestral state reconstruction suggests at least 9 independent origins of thermophily, with two distinct molecular strategies predominating across deep branches.",
            fieldTags = listOf("Evolutionary Biology", "Archaea", "Genomics", "Phylogenomics"),
            citationCount = 201,
            year = 2023,
            doi = "10.1038/s41586-023-06453-w",
            circleId = "c2",
            aiScore = 96,
            journal = "Nature"
        ),
        Paper(
            id = "p7",
            title = "Urban Carbon Flux Partitioning Using Eddy Covariance Networks: A Pan-Asian City Study",
            authors = listOf(users[6], users[10]),
            abstract = "Cities are net CO₂ sources whose carbon dynamics are poorly constrained in global climate models. We synthesize eddy covariance measurements from 21 urban monitoring stations across 11 Asian megacities (2019–2023) to partition anthropogenic and biogenic carbon fluxes. We find significant inter-city variance driven by vegetation fraction (r=0.71), building density (r=-0.63), and meteorological forcing. A random forest model trained on satellite-derived urban morphology predicts net ecosystem exchange with RMSE=1.2 gC m⁻² d⁻¹, enabling city-scale flux estimation without on-site sensors.",
            fieldTags = listOf("Climate Science", "Urban Ecology", "Carbon Flux", "Remote Sensing"),
            citationCount = 143,
            year = 2024,
            doi = "10.1038/s41558-024-01987-3",
            circleId = "c7",
            aiScore = 93,
            journal = "Nature Climate Change"
        ),
        Paper(
            id = "p8",
            title = "Perovskite Tandem Solar Cells with 31.2% Certified Efficiency via Interface Passivation Engineering",
            authors = listOf(users[7]),
            abstract = "Silicon-perovskite tandem solar cells have emerged as the most promising pathway beyond the single-junction efficiency limit. We report a certified power conversion efficiency of 31.2% for a 1 cm² four-terminal tandem device, achieved through a novel carboxyamidine self-assembled monolayer that passivates halide vacancies at the perovskite/electron transport layer interface. The passivation reduces non-radiative recombination by 78% and improves fill factor from 0.78 to 0.84. Devices show less than 5% degradation over 1,000 hours under 1-sun illumination at 85°C.",
            fieldTags = listOf("Solar Energy", "Perovskite", "Materials Science", "Energy Conversion"),
            citationCount = 456,
            year = 2024,
            doi = "10.1126/science.adl8953",
            circleId = "c7",
            aiScore = 98,
            journal = "Science"
        ),
        Paper(
            id = "p9",
            title = "Attention Mechanisms in Transformer Models: A Unified Mathematical Framework for Analysis and Pruning",
            authors = listOf(users[23], users[3]),
            abstract = "The proliferation of attention head variants in modern transformer architectures has outpaced our theoretical understanding of their computational properties. We develop a unified algebraic framework based on the theory of operator algebras that subsumes multi-head, grouped-query, sliding-window, and sparse attention as special cases. Within this framework, we prove that attention heads can be characterized by spectral properties of associated kernel matrices, and derive principled pruning criteria that remove 40% of heads with less than 1% accuracy degradation on GLUE benchmarks.",
            fieldTags = listOf("Deep Learning", "Transformers", "Theory", "NLP"),
            citationCount = 278,
            year = 2024,
            doi = "10.48550/arXiv.2404.11219",
            circleId = "c8",
            aiScore = 90,
            journal = "NeurIPS 2024"
        ),
        Paper(
            id = "p10",
            title = "The Replication Crisis in Social Psychology: A Meta-Analytical Update and Disciplinary Reform Agenda",
            authors = listOf(users[4], users[1]),
            abstract = "Building on the Open Science Collaboration's 2015 replication project, we conduct a comprehensive meta-analysis of 430 experimental social psychology studies published 2015–2023, coding for pre-registration, open data, and sample diversity. Our analysis reveals that the replication rate has improved modestly (from 39% to 54%) but remains substantially below acceptable scientific standards. Studies employing pre-registration and open materials replicate at 71% vs. 41% for non-pre-registered studies. We propose a tiered incentive structure for journals and funders to accelerate methodological reform.",
            fieldTags = listOf("Social Psychology", "Meta-Analysis", "Replication", "Open Science"),
            citationCount = 520,
            year = 2023,
            doi = "10.1177/09567976231212345",
            circleId = "c3",
            aiScore = 92,
            journal = "Psychological Science"
        ),
        Paper(
            id = "p11",
            title = "Decolonizing Cartography: Indigenous Land Knowledge Systems and GIS Integration in Canadian Arctic Communities",
            authors = listOf(users[16]),
            abstract = "Western cartographic practices impose coordinate-system abstractions that systematically erase Indigenous spatial knowledge and territorial relationships. We document a 3-year participatory design process with three Inuit communities in Nunavut, developing a hybrid GIS platform that encodes oral tradition, seasonal travel routes, and resource knowledge in formats controlled by community members. The platform enables legally defensible land claim documentation while respecting Indigenous data sovereignty protocols. Lessons for digital humanities practice and GIS tool design are drawn.",
            fieldTags = listOf("Indigenous Studies", "GIS", "Cartography", "Digital Humanities"),
            citationCount = 98,
            year = 2023,
            doi = "10.1080/14702541.2023.2234891",
            circleId = "c6",
            aiScore = 85,
            journal = "Cartographic Journal"
        ),
        Paper(
            id = "p12",
            title = "Multilingual Instruction Following in Low-Resource Languages: A Benchmark and Analysis",
            authors = listOf(users[27]),
            abstract = "Modern LLMs exhibit dramatic performance disparities across languages, yet systematic evaluation frameworks for low-resource language instruction following remain scarce. We introduce ML-Follow, a benchmark spanning 48 languages with fewer than 1 million Wikipedia articles, evaluating 12 commercially available and open-source LLMs. Performance on low-resource languages correlates strongly with web crawl representation (r=0.84) but not with training data claims from model cards. We release all data, evaluation code, and a structured error taxonomy for systematic analysis.",
            fieldTags = listOf("NLP", "Low-Resource Languages", "Benchmark", "LLM Evaluation"),
            citationCount = 187,
            year = 2024,
            doi = "10.48550/arXiv.2405.09821",
            circleId = "c8",
            aiScore = 89,
            journal = "EMNLP 2024"
        ),
        Paper(
            id = "p13",
            title = "Financial Inclusion and Mobile Money Adoption in West Africa: Causal Evidence from Nigeria's eNaira",
            authors = listOf(users[12]),
            abstract = "Central bank digital currencies (CBDCs) offer a policy lever for expanding financial inclusion, but causal evidence on their adoption dynamics is lacking. We exploit the phased rollout of Nigeria's eNaira as a quasi-natural experiment, using difference-in-differences estimation on administrative bank account data (N=8.2M individuals) and mobile operator records. eNaira availability increases banked population by 14.2 percentage points and mobile payment volumes by 31% in treated local government areas. Effects are concentrated among women and rural residents, consistent with a financial inclusion channel.",
            fieldTags = listOf("Economics", "CBDC", "Financial Inclusion", "Nigeria"),
            citationCount = 163,
            year = 2024,
            doi = "10.1093/restud/rdae022",
            circleId = "c3",
            aiScore = 91,
            journal = "Review of Economic Studies"
        ),
        Paper(
            id = "p14",
            title = "Fault-Tolerant Quantum Computation with Topological Codes on Near-Term Superconducting Processors",
            authors = listOf(users[2]),
            abstract = "Achieving fault-tolerant quantum computation requires physical error rates below a threshold determined by the chosen error-correcting code. We implement a distance-5 surface code on a 72-qubit superconducting processor and demonstrate a logical error rate of 4.3×10⁻⁹ per round of error correction at physical error rate p=0.003, below the threshold for this code size. A key innovation is a real-time classical decoder based on a convolutional neural network that operates with 100μs latency, enabling mid-circuit measurement feedback. These results represent a 100× improvement in logical error rate compared to our previous work.",
            fieldTags = listOf("Quantum Computing", "Error Correction", "Superconducting Qubits"),
            citationCount = 621,
            year = 2024,
            doi = "10.1038/s41586-024-07107-7",
            circleId = "c5",
            aiScore = 97,
            journal = "Nature"
        ),
        Paper(
            id = "p15",
            title = "Epigenetic Memory of Thermal Stress Across Generations in Arabidopsis: Evidence for Transgenerational Epigenetic Inheritance",
            authors = listOf(users[0], users[20], users[18]),
            abstract = "Whether environmentally-induced epigenetic changes can be transmitted across generations in plants remains highly contested. Using whole-genome bisulfite sequencing across 5 generations of Arabidopsis thaliana following heat stress, we identify 234 stably inherited differentially methylated regions (iDMRs) that persist for at least 4 stress-free generations. These iDMRs are enriched in promoters of stress-response genes and correlate with altered transcriptional priming under subsequent heat stress. Our findings provide mechanistic evidence for adaptive transgenerational epigenetic inheritance and suggest implications for understanding plant adaptation to climate change.",
            fieldTags = listOf("Epigenetics", "Plant Biology", "Climate Adaptation", "Genomics"),
            citationCount = 89,
            year = 2024,
            doi = "10.1126/sciadv.adm7891",
            circleId = "c2",
            aiScore = 94,
            journal = "Science Advances"
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // POSTS (~50 posts across different types)
    // ──────────────────────────────────────────────────────────────────────────

    val posts: List<Post> = listOf(
        // ── Paper shares ──
        Post(
            id = "post1",
            author = users[0],
            content = "Thrilled to share our latest work! After 18 months of fieldwork with 42 researchers, we've developed the 'distributed epistemic scaffolding' framework for understanding AI-augmented research. TL;DR: AI tools don't just assist cognition — they fundamentally restructure it. Four patterns emerged: delegation, negotiation, augmentation, and resistance. The resistance cases were the most fascinating. 🧵1/5",
            type = PostType.PAPER_SHARE,
            timestamp = System.currentTimeMillis() - 3_600_000L,
            endorseCount = 847,
            commentCount = 93,
            isEndorsed = true,
            circleId = "c1",
            circleName = "Machine Learning Frontiers",
            attachedPaper = papers[0]
        ),
        Post(
            id = "post2",
            author = users[18],
            content = "Huge step for health equity in genomics. Our PAGE Study collaboration shows PRS calibration across ancestry groups can be dramatically improved without new GWAS data. Please, let's stop deploying European-derived PRS in the clinic without adjustment. The code and calibrated weights are all open.",
            type = PostType.PAPER_SHARE,
            timestamp = System.currentTimeMillis() - 7_200_000L,
            endorseCount = 1203,
            commentCount = 147,
            isEndorsed = false,
            circleId = "c2",
            circleName = "Genomics & Precision Medicine",
            attachedPaper = papers[1]
        ),
        Post(
            id = "post3",
            author = users[7],
            content = "31.2% certified efficiency. Silicon-perovskite tandems are approaching their theoretical ceiling faster than anyone predicted. The interface passivation monolayer we developed took 14 tries to get right — science is sometimes mostly chemistry intuition and persistence.",
            type = PostType.PAPER_SHARE,
            timestamp = System.currentTimeMillis() - 14_400_000L,
            endorseCount = 2891,
            commentCount = 204,
            isEndorsed = true,
            circleId = "c7",
            circleName = "Climate & Sustainability Science",
            attachedPaper = papers[7]
        ),
        Post(
            id = "post4",
            author = users[3],
            content = "Does RLHF actually teach models to reason causally, or does it teach them to sound more causal? Spoiler: mostly the latter on OOD inputs. This has significant implications for deployment in medical and legal reasoning contexts where causality matters most.",
            type = PostType.PAPER_SHARE,
            timestamp = System.currentTimeMillis() - 21_600_000L,
            endorseCount = 967,
            commentCount = 128,
            isEndorsed = false,
            circleId = "c8",
            circleName = "NLP & Language Technologies",
            attachedPaper = papers[3]
        ),
        // ── Discussion posts ──
        Post(
            id = "post5",
            author = users[4],
            content = "Hot take: the academic conference system is fundamentally broken for hybrid events. I just attended a 'hybrid' conference where remote attendees couldn't participate in discussions, workshops, or networking. We paid full registration for a Zoom watch party. Can we crowdsource a better model? What's actually worked for your community?",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 43_200_000L,
            endorseCount = 1456,
            commentCount = 232,
            isEndorsed = true,
            circleId = "c1",
            circleName = "Machine Learning Frontiers",
            flair = PostFlair.DISCUSSION
        ),
        Post(
            id = "post6",
            author = users[11],
            content = "Concerned about a trend I'm seeing in preprint epidemiology: studies with n<500 making broad population-level claims, circulating on social media before peer review. I understand the urgency of rapid communication, but we need better community norms around preprint interpretation. Thoughts from the epidemiology community?",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 86_400_000L,
            endorseCount = 3200,
            commentCount = 418,
            isEndorsed = true,
            circleId = "c2",
            circleName = "Genomics & Precision Medicine",
            flair = PostFlair.DISCUSSION
        ),
        Post(
            id = "post7",
            author = users[9],
            content = "Can someone recommend good resources for getting started with molecular dynamics simulations? I understand the biological background (protein folding, membrane dynamics) but feel lost on the computational setup. Should I start with GROMACS or AMBER? Running on a university HPC cluster.",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 172_800_000L,
            endorseCount = 89,
            commentCount = 34,
            isEndorsed = false,
            circleId = "c2",
            circleName = "Genomics & Precision Medicine",
            flair = PostFlair.QUESTION
        ),
        Post(
            id = "post8",
            author = users[19],
            content = "Genuinely excited about the state of open science in 2024. Preregistration rates up, open data mandates expanding, replication projects proliferating. The culture is changing, even if it's slower than we'd like. Staying optimistic.",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 259_200_000L,
            endorseCount = 2100,
            commentCount = 89,
            isEndorsed = true,
            flair = PostFlair.DISCUSSION
        ),
        // ── Milestones ──
        Post(
            id = "post9",
            author = users[0],
            content = "",
            type = PostType.MILESTONE,
            timestamp = System.currentTimeMillis() - 302_400_000L,
            endorseCount = 892,
            commentCount = 67,
            isEndorsed = false,
            milestoneText = "🎉 Dr. Reyes' paper 'Polygenic Risk Score Calibration' just reached 300 citations — congratulations!"
        ),
        Post(
            id = "post10",
            author = users[23],
            content = "",
            type = PostType.MILESTONE,
            timestamp = System.currentTimeMillis() - 432_000_000L,
            endorseCount = 1540,
            commentCount = 112,
            isEndorsed = true,
            milestoneText = "🏆 Prof. Yamamoto's cumulative citation count has crossed 45,000 — a remarkable body of work in AI!"
        ),
        // ── More discussions ──
        Post(
            id = "post11",
            author = users[12],
            content = "I've been trying to apply NLP methods to 19th-century German correspondence and the OCR quality is... humbling. Has anyone worked with noisy historical text? The character error rates from historical document scans (4–12%) break most modern NLP pipelines. Happy to share my preprocessing workflow if others are interested.",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 518_400_000L,
            endorseCount = 345,
            commentCount = 58,
            isEndorsed = false,
            circleId = "c6",
            circleName = "Digital Humanities Lab",
            flair = PostFlair.QUESTION
        ),
        Post(
            id = "post12",
            author = users[5],
            content = "New analysis: in a sample of 1,200 high-impact papers from 2022, papers with open data were cited 47% more than closed-data equivalents in the same journals, after controlling for author prestige and journal tier. The open science citation advantage is real and growing. Thread on methodology below.",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 604_800_000L,
            endorseCount = 4100,
            commentCount = 289,
            isEndorsed = true
        ),
        Post(
            id = "post13",
            author = users[2],
            content = "Just finished a month-long review marathon (7 papers). Some reflections: the variance in review quality from co-reviewers is staggering. One co-reviewer returned a 20-line opinion on a 40-page theory paper. Another wrote a 6-page structured review for a 3-page workshop abstract. How do we train reviewers better? Or should it be structural incentives?",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 691_200_000L,
            endorseCount = 1870,
            commentCount = 193,
            isEndorsed = true,
            flair = PostFlair.DISCUSSION
        ),
        Post(
            id = "post14",
            author = users[13],
            content = "We've been told for years that quantum advantage requires error correction. Our new results (31.2% below threshold at distance 5) suggest the timeline may be shorter than the pessimistic consensus. Still not claiming 'quantum supremacy' for anything useful — but the threshold crossings are becoming routine.",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 777_600_000L,
            endorseCount = 3456,
            commentCount = 310,
            isEndorsed = false,
            circleId = "c5",
            circleName = "Quantum Information & Computing"
        ),
        Post(
            id = "post15",
            author = users[14],
            content = "Academia can be lonely, especially in the first two years of a PhD. Reminder that struggling ≠ failing. If you're in the 'imposter syndrome valley,' you're in very good company — most of the researchers you admire have been there. Reach out to your community here. 💙",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 864_000_000L,
            endorseCount = 8900,
            commentCount = 445,
            isEndorsed = true
        ),
        Post(
            id = "post16",
            author = users[6],
            content = "Results from our Ghana randomized trial are in. TL;DR: offline-capable mobile learning works, but teacher training is the real bottleneck. Apps don't teach — teachers do. Technology amplifies good pedagogy but doesn't replace it. Sobering and encouraging at the same time.",
            type = PostType.PAPER_SHARE,
            timestamp = System.currentTimeMillis() - 950_400_000L,
            endorseCount = 723,
            commentCount = 78,
            isEndorsed = false,
            circleId = "c4",
            circleName = "Open Education Research",
            attachedPaper = papers[4]
        ),
        Post(
            id = "post17",
            author = users[15],
            content = "Question for the psych community: what is the best paradigm for studying moral reasoning cross-culturally while minimizing WEIRD sample bias? I'm comparing Brazilian, Indian, and Swedish undergrads and already feeling the floor/ceiling effect issues on standard trolley-problem-derived instruments.",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 1_036_800_000L,
            endorseCount = 234,
            commentCount = 67,
            isEndorsed = false,
            circleId = "c3",
            circleName = "Cognitive & Behavioral Sciences",
            flair = PostFlair.QUESTION
        ),
        Post(
            id = "post18",
            author = users[17],
            content = "Unexpected methodological rabbit hole: trying to computationally reconstruct the social network of botanical correspondents in 1780s London. The letters exist (scattered across 11 archives in 3 countries), the digitization is partial, the OCR is imperfect, and named entity recognition fails on 18th-century spelling conventions. Progress is measured in years, not months.",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 1_123_200_000L,
            endorseCount = 412,
            commentCount = 43,
            isEndorsed = false,
            circleId = "c6",
            circleName = "Digital Humanities Lab"
        ),
        Post(
            id = "post19",
            author = currentUser,
            content = "Working on the revision of our epistemic scaffolding paper based on CHI reviews. Reviewers pushed back on our operationalization of 'cognitive load' — fairly, I think. We were conflating subjective workload with objective task complexity. Going back to the raw experience-sampling data to re-code. Science is iterative. 🔁",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 1_209_600_000L,
            endorseCount = 567,
            commentCount = 34,
            isEndorsed = false
        ),
        Post(
            id = "post20",
            author = users[8],
            content = "I keep getting asked: 'when will perovskite solar cells be commercially available?' The honest answer is 3–7 years, and the main bottleneck isn't efficiency — it's stability under real-world conditions and lead-free alternatives. The 31.2% cell is 1 cm² in a controlled lab environment. Scale-up physics are brutal.",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 1_296_000_000L,
            endorseCount = 1234,
            commentCount = 98,
            isEndorsed = true,
            circleId = "c7",
            circleName = "Climate & Sustainability Science"
        ),
        Post(
            id = "post21",
            author = users[27],
            content = "Released ML-Follow today — 48 low-resource language benchmark for instruction following. The disparity between high-resource (English) and low-resource language performance in SOTA LLMs is 43 percentage points on average. This is a fairness problem, not just a performance problem.",
            type = PostType.PAPER_SHARE,
            timestamp = System.currentTimeMillis() - 1_382_400_000L,
            endorseCount = 1892,
            commentCount = 167,
            isEndorsed = false,
            circleId = "c8",
            circleName = "NLP & Language Technologies",
            attachedPaper = papers[11]
        ),
        Post(
            id = "post22",
            author = users[22],
            content = "Masters thesis question: looking for literature on community health worker effectiveness interventions in East Africa. Specifically interested in supervision models and incentive structures. The WHO guidance is helpful but quite general. Any recommended papers or researchers working on this?",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 1_468_800_000L,
            endorseCount = 145,
            commentCount = 28,
            isEndorsed = false,
            flair = PostFlair.QUESTION
        ),
        Post(
            id = "post23",
            author = users[20],
            content = "8,124 archaeal genomes, 9 independent origins of thermophily, two molecular strategies. Ancient life keeps surprising us. The convergent evolution of heat tolerance is so robust it's almost predictable — yet the underlying genomic paths are divergent. Evolution finds the same peaks through different valleys.",
            type = PostType.PAPER_SHARE,
            timestamp = System.currentTimeMillis() - 1_555_200_000L,
            endorseCount = 678,
            commentCount = 56,
            isEndorsed = true,
            circleId = "c2",
            circleName = "Genomics & Precision Medicine",
            attachedPaper = papers[5]
        ),
        Post(
            id = "post24",
            author = users[28],
            content = "Applying for my first faculty position. The process is opaque to an almost comical degree. Does anyone have experience navigating the difference between R1 research university and teaching-focused institutions? What did you wish you'd known about the job talk?",
            type = PostType.DISCUSSION,
            timestamp = System.currentTimeMillis() - 1_641_600_000L,
            endorseCount = 890,
            commentCount = 134,
            isEndorsed = false,
            flair = PostFlair.QUESTION
        ),
        Post(
            id = "post25",
            author = users[10],
            content = "Pan-Asian urban carbon flux synthesis: 21 stations, 11 megacities, 5 years of data. We can now predict net ecosystem exchange from satellite imagery alone with surprising accuracy. The implications for emissions monitoring and MRV (measurement, reporting, verification) under the Paris Agreement are significant.",
            type = PostType.PAPER_SHARE,
            timestamp = System.currentTimeMillis() - 1_728_000_000L,
            endorseCount = 892,
            commentCount = 74,
            isEndorsed = false,
            circleId = "c7",
            circleName = "Climate & Sustainability Science",
            attachedPaper = papers[6]
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // COMMENTS
    // ──────────────────────────────────────────────────────────────────────────

    fun getCommentsForPost(postId: String): List<Comment> = when (postId) {
        "post1" -> listOf(
            Comment(
                id = "cm1",
                author = users[1],
                content = "The 'resistance' pattern you identified resonates deeply with my experience in biology. Several of my graduate students actively refuse to use LLM assistance for literature synthesis, citing epistemic authenticity concerns. I'm now studying this as a phenomenon in its own right.",
                timestamp = System.currentTimeMillis() - 2_400_000L,
                likeCount = 124,
                replyCount = 2,
                replies = listOf(
                    Comment(
                        id = "cm1r1",
                        author = currentUser,
                        content = "Exactly! And the interesting thing is that resistance isn't necessarily irrational — several of our 'resisters' produced the most methodologically rigorous work. We're exploring whether resistance functions as an epistemic identity marker.",
                        timestamp = System.currentTimeMillis() - 1_800_000L,
                        likeCount = 87,
                        parentId = "cm1"
                    ),
                    Comment(
                        id = "cm1r2",
                        author = users[4],
                        content = "Would love to see a follow-up study disaggregating by career stage. I hypothesize resistance is higher in senior researchers who've already developed strong disciplinary identities.",
                        timestamp = System.currentTimeMillis() - 1_200_000L,
                        likeCount = 45,
                        parentId = "cm1"
                    )
                )
            ),
            Comment(
                id = "cm2",
                author = users[5],
                content = "This connects to Sheila Jasanoff's work on 'co-production' of science and society. The epistemic scaffolding concept maps well onto sociotechnical imaginaries. Have you engaged with STS literature in your framework development?",
                timestamp = System.currentTimeMillis() - 2_100_000L,
                likeCount = 89,
                replyCount = 1,
                replies = listOf(
                    Comment(
                        id = "cm2r1",
                        author = currentUser,
                        content = "Yes, Jasanoff was foundational for our theoretical framing! We also drew heavily on Hutchins' distributed cognition and Pickering's 'mangle of practice.' The paper has a full STS literature review in the supplementary.",
                        timestamp = System.currentTimeMillis() - 1_500_000L,
                        likeCount = 67,
                        parentId = "cm2"
                    )
                )
            ),
            Comment(
                id = "cm3",
                author = users[16],
                content = "Methodologically, how did you handle the ethical complexity of screen recording researchers? Did you find that observation affected the behaviors you were studying? (The Hawthorne effect concern seems significant here.)",
                timestamp = System.currentTimeMillis() - 1_800_000L,
                likeCount = 56,
                replyCount = 0
            )
        )
        "post5" -> listOf(
            Comment(
                id = "cm4",
                author = users[10],
                content = "NeurIPS 2023 actually did this quite well: asynchronous poster sessions recorded in advance, dedicated Gather.town spaces, and importantly — a 30% hybrid attendance ticket discount. Attendance and participation metrics improved dramatically for remote attendees.",
                timestamp = System.currentTimeMillis() - 36_000_000L,
                likeCount = 234,
                replyCount = 0
            ),
            Comment(
                id = "cm5",
                author = users[5],
                content = "From the sociology of science perspective: hybrid conferences reproduce and amplify existing hierarchies. Presenters get 100% of the room's attention; remote attendees exist as a second-class stream. Until we redesign the fundamental format rather than adding tech on top, it won't change.",
                timestamp = System.currentTimeMillis() - 30_000_000L,
                likeCount = 567,
                replyCount = 2,
                replies = listOf(
                    Comment(
                        id = "cm5r1",
                        author = users[4],
                        content = "What would a genuine format redesign look like? I've been part of a few 'async-first' conferences where all presentations were pre-recorded and discussion happened over 2 weeks. Surprisingly effective for depth, though the spontaneity was missing.",
                        timestamp = System.currentTimeMillis() - 24_000_000L,
                        likeCount = 123,
                        parentId = "cm5"
                    ),
                    Comment(
                        id = "cm5r2",
                        author = users[5],
                        content = "That async model is interesting but creates its own inequalities — those with time to engage throughout get much more. Maybe a tiered model: core sessions sync, breakouts async with structured facilitation?",
                        timestamp = System.currentTimeMillis() - 18_000_000L,
                        likeCount = 89,
                        parentId = "cm5"
                    )
                )
            )
        )
        else -> listOf(
            Comment(
                id = "cmDefault1",
                author = users[(postId.hashCode().absoluteValue) % users.size],
                content = "Really insightful work — looking forward to seeing how this develops. The methodological approach is particularly well-suited to the research question.",
                timestamp = System.currentTimeMillis() - 600_000L,
                likeCount = (postId.hashCode().absoluteValue) % 200 + 10,
                replyCount = 0
            )
        )
    }

    private val Int.absoluteValue get() = if (this < 0) -this else this

    // ──────────────────────────────────────────────────────────────────────────
    // NOTIFICATIONS
    // ──────────────────────────────────────────────────────────────────────────

    val notifications = listOf(
        Notification(
            id = "n1",
            type = NotifType.ENDORSEMENT,
            actor = users[0],
            content = "Dr. Elena Reyes endorsed your post on epistemic scaffolding",
            timestamp = System.currentTimeMillis() - 1_800_000L,
            isRead = false,
            targetId = "post1"
        ),
        Notification(
            id = "n2",
            type = NotifType.COMMENT,
            actor = users[5],
            content = "Dr. Sophie Laurent commented on your post: \"This connects to Jasanoff's work...\"",
            timestamp = System.currentTimeMillis() - 3_600_000L,
            isRead = false,
            targetId = "post1"
        ),
        Notification(
            id = "n3",
            type = NotifType.AI_APPROVED,
            actor = users[0], // system actor
            content = "Your paper 'Situated Cognition in AI-Augmented Knowledge Work' passed AI pre-review with a score of 87/100",
            timestamp = System.currentTimeMillis() - 7_200_000L,
            isRead = false,
            targetId = "p1"
        ),
        Notification(
            id = "n4",
            type = NotifType.CONNECTION,
            actor = users[2],
            content = "Aisha Nakamura accepted your connection request",
            timestamp = System.currentTimeMillis() - 14_400_000L,
            isRead = true,
            targetId = "u3"
        ),
        Notification(
            id = "n5",
            type = NotifType.CITATION,
            actor = users[1],
            content = "Dr. Elena Reyes cited your paper in 'Genomic methods for distributed cognition research'",
            timestamp = System.currentTimeMillis() - 86_400_000L,
            isRead = true,
            targetId = "p1"
        ),
        Notification(
            id = "n6",
            type = NotifType.CIRCLE_INVITE,
            actor = users[16],
            content = "Dr. Rajesh Patel invited you to join the AI Ethics & Society circle",
            timestamp = System.currentTimeMillis() - 172_800_000L,
            isRead = true,
            targetId = "c1"
        ),
        Notification(
            id = "n7",
            type = NotifType.ENDORSEMENT,
            actor = users[3],
            content = "Carlos Mendoza, Priya Venkataraman, and 12 others endorsed your paper",
            timestamp = System.currentTimeMillis() - 259_200_000L,
            isRead = true,
            targetId = "p1"
        ),
        Notification(
            id = "n8",
            type = NotifType.NEW_FOLLOWER,
            actor = users[23],
            content = "Prof. Hiroshi Yamamoto started following you",
            timestamp = System.currentTimeMillis() - 345_600_000L,
            isRead = true,
            targetId = "u23"
        ),
        Notification(
            id = "n9",
            type = NotifType.COMMENT,
            actor = users[10],
            content = "Dr. Noah Bergman replied to your comment in Machine Learning Frontiers",
            timestamp = System.currentTimeMillis() - 432_000_000L,
            isRead = true,
            targetId = "post5"
        ),
        Notification(
            id = "n10",
            type = NotifType.CITATION,
            actor = users[19],
            content = "Sunita Krishnaswamy cited your work in a preprint on genomic data sharing",
            timestamp = System.currentTimeMillis() - 604_800_000L,
            isRead = true,
            targetId = "p1"
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // CONVERSATIONS & MESSAGES
    // ──────────────────────────────────────────────────────────────────────────

    val conversations = listOf(
        Conversation(
            id = "conv_ai",
            participants = listOf(currentUser, aiUser),
            lastMessage = Message(
                id = "m_ai_start",
                senderId = "ai_copilot",
                content = "Hello! I am your CiteCircle AI Copilot. Ask me any research question, ask for paper summaries, or methodology advice.",
                timestamp = System.currentTimeMillis() - 86_400_000L
            ),
            unreadCount = 0
        ),
        Conversation(
            id = "conv1",
            participants = listOf(currentUser, users[0]),
            lastMessage = Message(
                id = "m1",
                senderId = users[0].id,
                content = "Looking forward to the collaboration! Should we set up a shared drive?",
                timestamp = System.currentTimeMillis() - 1_800_000L
            ),
            unreadCount = 2
        ),
        Conversation(
            id = "conv2",
            participants = listOf(currentUser, users[2]),
            lastMessage = Message(
                id = "m2",
                senderId = currentUser.id,
                content = "Thanks for connecting! Your work on quantum error correction is fascinating.",
                timestamp = System.currentTimeMillis() - 86_400_000L
            ),
            unreadCount = 0
        ),
        Conversation(
            id = "conv3",
            participants = listOf(currentUser, users[5]),
            lastMessage = Message(
                id = "m3",
                senderId = users[5].id,
                content = "The citation advantage paper would love to have your HCI perspective. Coffee chat this week?",
                timestamp = System.currentTimeMillis() - 172_800_000L
            ),
            unreadCount = 1
        ),
        Conversation(
            id = "conv4",
            participants = listOf(currentUser, users[18]),
            lastMessage = Message(
                id = "m4",
                senderId = users[18].id,
                content = "I saw your CHI paper — would you be open to co-authoring a methods chapter on experience sampling?",
                timestamp = System.currentTimeMillis() - 259_200_000L
            ),
            unreadCount = 0
        ),
        Conversation(
            id = "conv5",
            participants = listOf(currentUser, users[3]),
            lastMessage = Message(
                id = "m5",
                senderId = currentUser.id,
                content = "Yes, the causal disentanglement findings align well with what we found in researcher tool adoption patterns!",
                timestamp = System.currentTimeMillis() - 345_600_000L
            ),
            unreadCount = 0
        )
    )

    fun getMessagesForConversation(convId: String): List<Message> = when (convId) {
        "conv_ai" -> listOf(
            Message(
                id = "m_ai_start",
                senderId = "ai_copilot",
                content = "Hello! I am your CiteCircle AI Copilot. Ask me any research question, ask for paper summaries, or methodology advice.",
                timestamp = System.currentTimeMillis() - 86_400_000L
            )
        )
        "conv1" -> listOf(
            Message("m1a", currentUser.id, "Hi Elena! Just saw your latest preprint on PRS calibration.", System.currentTimeMillis() - 5_400_000L),
            Message("m1b", users[0].id, "Thanks Maya! It was a big push to get it out before ASHG.", System.currentTimeMillis() - 4_800_000L),
            Message("m1c", currentUser.id, "Would love to explore overlaps between your ancestry calibration work and my tool adoption research — different populations, same equity questions.", System.currentTimeMillis() - 3_600_000L),
            Message("m1d", users[0].id, "That's a really interesting angle. I hadn't thought about it that way.", System.currentTimeMillis() - 2_400_000L),
            Message("m1e", users[0].id, "Looking forward to the collaboration! Should we set up a shared drive?", System.currentTimeMillis() - 1_800_000L, attachedPaper = papers[1])
        )
        "conv3" -> listOf(
            Message("m3a", users[5].id, "Maya, I've been following your work on AI-augmented research tools.", System.currentTimeMillis() - 259_200_000L),
            Message("m3b", currentUser.id, "Sophie! Your citation advantage paper is cited in my next draft, actually.", System.currentTimeMillis() - 255_600_000L),
            Message("m3c", users[5].id, "That means a lot. The open science community needs more empirical HCI work.", System.currentTimeMillis() - 252_000_000L),
            Message("m3d", users[5].id, "The citation advantage paper would love to have your HCI perspective. Coffee chat this week?", System.currentTimeMillis() - 172_800_000L)
        )
        else -> listOf(
            Message("mDefault1", currentUser.id, "Great connecting!", System.currentTimeMillis() - 600_000_000L),
            Message("mDefault2", conversations.find { it.id == convId }?.participants?.firstOrNull { it.id != currentUser.id }?.id ?: "u1",
                "Likewise! Looking forward to following your work.", System.currentTimeMillis() - 500_000_000L)
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AI REVIEW (canned report returned after staged delay)
    // ──────────────────────────────────────────────────────────────────────────

    val sampleAiReport = AiReviewReport(
        score = 82,
        structure = 88,
        citations = 79,
        clarity = 85,
        originality = 76,
        suggestions = listOf(
            AiSuggestion(
                id = "s1",
                section = "Abstract",
                text = "The abstract does not state the primary finding quantitatively. Consider adding the key effect size or result metric to help readers quickly assess contribution.",
                severity = Severity.MODERATE,
                isAddressed = false
            ),
            AiSuggestion(
                id = "s2",
                section = "Related Work",
                text = "Several recent papers (2023–2024) in your cited area are not referenced. Consider a final literature check to ensure currency of your review.",
                severity = Severity.MINOR,
                isAddressed = false
            ),
            AiSuggestion(
                id = "s3",
                section = "Methodology",
                text = "The sample size justification references power analysis for a different primary outcome than the one analyzed. This inconsistency may raise reviewer concerns.",
                severity = Severity.NEEDS_ATTENTION,
                isAddressed = false
            ),
            AiSuggestion(
                id = "s4",
                section = "Results",
                text = "Figure 3 caption does not specify error bar type (SE vs. SD vs. 95% CI). Add this information for reproducibility.",
                severity = Severity.MINOR,
                isAddressed = false
            ),
            AiSuggestion(
                id = "s5",
                section = "Discussion",
                text = "The limitations section does not address potential demand characteristics in the self-report measures. This is a standard concern for this methodology.",
                severity = Severity.MODERATE,
                isAddressed = false
            ),
            AiSuggestion(
                id = "s6",
                section = "Conclusion",
                text = "The conclusion makes a policy recommendation that is not directly supported by the study's data. Consider softening language or adding caveats.",
                severity = Severity.NEEDS_ATTENTION,
                isAddressed = false
            )
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // SUGGESTED PEOPLE (for network/onboarding discovery)
    // ──────────────────────────────────────────────────────────────────────────

    val suggestedPeopleForOnboarding = users.take(6)
    val suggestedCirclesForOnboarding = circles.take(4)

    val connectionRequests = listOf(
        users[4],   // Carlos — student
        users[12],  // Lena — student
        users[25]   // Dr. Ahmed
    )

    val suggestedConnections = users.filter { !it.isConnected && !it.isFollowing }.take(8)
}
