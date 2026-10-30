/** Fictional, bundled catalog. Replace the data boundary when a provider is introduced. */
export interface Company {
  id: string; name: string; initials: string; sector: string; location: string;
  size: string; model: string; desc: string; about: string; values: string[];
  benefits: string[]; color: string;
}
export interface Opportunity {
  id: string; title: string; company: string; role: string; mode: string;
  city: string; location: string; salary: number; max: number; experience: string;
  minimumYears: number; officeDays: number; tags: string[]; line: string; intro: string;
  tasks: string[]; requirements: string[]; unknown: string; published: string;
}
export const companies: Company[] = [
  {
    "id": "northline",
    "name": "Northline Studio",
    "initials": "ns",
    "sector": "Product engineering",
    "location": "Madrid, Spain",
    "size": "51–100 people",
    "model": "Remote first",
    "desc": "Software for independent businesses. Small product teams, clear ownership and time to build things well.",
    "about": "Northline Studio builds the tools independent businesses use to manage their day. Engineers work alongside designers and product managers, with a focus on maintainable systems and practical decisions.",
    "values": [
      "Small, cross-functional teams",
      "Written decisions and flexible schedules",
      "A learning budget and regular pairing"
    ],
    "benefits": [
      "Flexible start and finish times",
      "€1,200 annual learning budget",
      "25 days of annual leave"
    ],
    "color": "forest"
  },
  {
    "id": "vela",
    "name": "Vela Systems",
    "initials": "ve",
    "sector": "Climate & infrastructure",
    "location": "Valencia, Spain",
    "size": "21–50 people",
    "model": "Hybrid",
    "desc": "Useful technology for a more efficient energy system. A close team working across software, data and design.",
    "about": "Vela Systems helps local operators understand energy demand and plan infrastructure. The team brings together software engineers, data specialists and people with experience in the energy sector.",
    "values": [
      "Direct access to product decisions",
      "Two shared office days each week",
      "A predictable, sustainable pace"
    ],
    "benefits": [
      "Flexible hours",
      "Public transport allowance",
      "A dedicated development budget"
    ],
    "color": "sage"
  },
  {
    "id": "cedar",
    "name": "Cedar Labs",
    "initials": "cl",
    "sector": "Business software",
    "location": "Barcelona, Spain",
    "size": "101–200 people",
    "model": "Flexible by team",
    "desc": "Reliable software for complex operations. Established engineering practices with room to improve how work gets done.",
    "about": "Cedar Labs develops business systems for logistics and operations teams. Its engineering groups own services from design through delivery, with a shared platform and specialist support.",
    "values": [
      "Documented engineering standards",
      "Dedicated time for technical improvements",
      "Team-led working agreements"
    ],
    "benefits": [
      "Private health cover",
      "Language classes",
      "27 days of annual leave"
    ],
    "color": "sand"
  }
];
export const opportunities: Opportunity[] = [
  {
    "id": "backend-java",
    "title": "Backend Developer",
    "company": "northline",
    "role": "Backend",
    "mode": "Remote",
    "city": "Madrid",
    "location": "Spain",
    "salary": 46000,
    "max": 58000,
    "experience": "2+ years",
    "tags": [
      "Java",
      "Spring Boot",
      "PostgreSQL"
    ],
    "line": "Build reliable services for independent businesses.",
    "intro": "Help shape the services behind a product used by independent businesses. You will join a small team that values readable code, thoughtful reviews and dependable releases.",
    "tasks": [
      "Build and maintain Java services and REST APIs.",
      "Work with product and frontend teams on well-defined increments.",
      "Improve automated tests, observability and database performance."
    ],
    "requirements": [
      "At least two years working with Java in a professional team.",
      "Practical experience with Spring Boot and relational databases.",
      "Comfort discussing technical decisions and reviewing code."
    ],
    "unknown": "Working language and interview accommodations are not specified.",
    "minimumYears": 2,
    "officeDays": 0,
    "published": "2026-10-28"
  },
  {
    "id": "frontend-react",
    "title": "Frontend Developer",
    "company": "northline",
    "role": "Frontend",
    "mode": "Remote",
    "city": "Madrid",
    "location": "Spain",
    "salary": 42000,
    "max": 54000,
    "experience": "2+ years",
    "tags": [
      "React",
      "TypeScript",
      "CSS"
    ],
    "line": "Make everyday business tools clearer and easier to use.",
    "intro": "Work closely with design and backend colleagues to turn complex workflows into understandable interfaces. Accessibility and responsiveness are part of the work from the start.",
    "tasks": [
      "Build responsive product interfaces in React.",
      "Collaborate on an accessible, consistent component library.",
      "Test important user journeys and improve performance."
    ],
    "requirements": [
      "Two years of frontend development experience.",
      "Confidence with TypeScript, CSS and browser behavior.",
      "Experience translating designs into accessible interfaces."
    ],
    "unknown": "The expected level of spoken English is not specified.",
    "minimumYears": 2,
    "officeDays": 0,
    "published": "2026-10-27"
  },
  {
    "id": "data-engineer",
    "title": "Data Engineer",
    "company": "vela",
    "role": "Data",
    "mode": "Hybrid",
    "city": "Valencia",
    "location": "Valencia, Spain",
    "salary": 44000,
    "max": 56000,
    "experience": "2+ years",
    "tags": [
      "Python",
      "SQL",
      "dbt"
    ],
    "line": "Turn energy data into useful, trustworthy information.",
    "intro": "Build the datasets that help infrastructure teams make better decisions. You will work on clear data contracts, reliable pipelines and a practical approach to quality.",
    "tasks": [
      "Develop tested pipelines for operational and analytical data.",
      "Define data quality checks with product specialists.",
      "Document datasets and improve pipeline observability."
    ],
    "requirements": [
      "Experience with Python and SQL in data workflows.",
      "Familiarity with dimensional modeling and transformation tools.",
      "Availability for two office days a week in Valencia."
    ],
    "unknown": "Relocation support is not specified.",
    "minimumYears": 2,
    "officeDays": 2,
    "published": "2026-10-29"
  },
  {
    "id": "fullstack",
    "title": "Full Stack Developer",
    "company": "vela",
    "role": "Full stack",
    "mode": "Hybrid",
    "city": "Valencia",
    "location": "Valencia, Spain",
    "salary": 40000,
    "max": 52000,
    "experience": "2+ years",
    "tags": [
      "TypeScript",
      "React",
      "Node.js"
    ],
    "line": "Connect energy insights with the people who need them.",
    "intro": "Own small product features from the interface to the service layer. The team works in short, focused increments with regular feedback from people in the field.",
    "tasks": [
      "Deliver features across React interfaces and Node.js services.",
      "Collaborate on API contracts and straightforward data models.",
      "Take part in reviews, testing and production support."
    ],
    "requirements": [
      "Experience delivering features across a web stack.",
      "Solid TypeScript skills and familiarity with relational data.",
      "Availability for two office days a week in Valencia."
    ],
    "unknown": "On-call expectations are not specified.",
    "minimumYears": 2,
    "officeDays": 2,
    "published": "2026-10-26"
  },
  {
    "id": "platform-engineer",
    "title": "Platform Engineer",
    "company": "cedar",
    "role": "Platform",
    "mode": "Remote",
    "city": "Barcelona",
    "location": "Spain",
    "salary": 55000,
    "max": 68000,
    "experience": "4+ years",
    "tags": [
      "Kubernetes",
      "Terraform",
      "Linux"
    ],
    "line": "Give engineering teams a dependable place to build.",
    "intro": "Improve the shared platform that supports several product teams. Focus on repeatable infrastructure, useful observability and a developer experience that removes friction.",
    "tasks": [
      "Maintain infrastructure and delivery pipelines.",
      "Improve service reliability and operational documentation.",
      "Work with product teams on safe deployment practices."
    ],
    "requirements": [
      "Four years of infrastructure or platform experience.",
      "Practical Kubernetes, Terraform and Linux knowledge.",
      "Experience with incident response and service monitoring."
    ],
    "unknown": "On-call frequency and compensation are not specified.",
    "minimumYears": 4,
    "officeDays": 0,
    "published": "2026-10-25"
  },
  {
    "id": "sap-developer",
    "title": "SAP Developer",
    "company": "cedar",
    "role": "SAP",
    "mode": "On-site",
    "city": "Barcelona",
    "location": "Barcelona, Spain",
    "salary": 38000,
    "max": 49000,
    "experience": "1+ year",
    "tags": [
      "ABAP",
      "SAP S/4HANA",
      "SQL"
    ],
    "line": "Build maintainable integrations for everyday operations.",
    "intro": "Join a team modernizing business workflows and integrations. The role combines hands-on development with a close understanding of how operations teams use their tools.",
    "tasks": [
      "Develop and maintain ABAP applications and integrations.",
      "Work with functional colleagues to clarify requirements.",
      "Document changes and support reliable release processes."
    ],
    "requirements": [
      "At least one year of practical ABAP development.",
      "Familiarity with SAP business processes and SQL.",
      "Availability to work on-site in Barcelona."
    ],
    "unknown": "Future hybrid arrangements are not specified.",
    "minimumYears": 1,
    "officeDays": 5,
    "published": "2026-10-24"
  }
];
export const findCompany = (id: string) => companies.find(company => company.id === id);
export const findOpportunity = (id: string) => opportunities.find(job => job.id === id);
export const salaryLabel = (job: Opportunity) => '€' + job.salary / 1000 + 'k–€' + job.max / 1000 + 'k';
