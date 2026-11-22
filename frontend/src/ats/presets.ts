import type { ProfessionalProfile } from '../profile/model';

export type StackPresetId =
  | 'all-tech'
  | 'fullstack'
  | 'python-data'
  | 'ai-ml'
  | 'cloud-devops'
  | 'qa-cyber'
  | 'sap'
  | 'custom';

export interface StackPreset {
  id: StackPresetId;
  label: string;
  shortLabel: string;
  description: string;
  keywords: readonly string[];
}

export const CUSTOM_STACK_STORAGE_KEY = 'jobsinton.ats-custom-stack';

export const FULLSTACK_KEYWORDS = [
  'Java', 'Spring Boot', 'JavaScript', 'TypeScript', 'React', 'Angular', 'Vue.js', 'Node.js',
  'Next.js', 'C#', '.NET', 'PHP', 'Kotlin', 'Swift', 'Flutter', 'PostgreSQL', 'MySQL',
  'MongoDB', 'REST', 'GraphQL', 'Docker',
] as const;

export const PYTHON_DATA_KEYWORDS = [
  'Python', 'Data Analyst', 'Data Engineer', 'Data Scientist', 'Business Intelligence',
  'SQL', 'Power BI', 'Tableau', 'Looker', 'Pandas', 'Django', 'FastAPI', 'Flask',
  'Spark', 'PySpark', 'Databricks', 'Snowflake', 'BigQuery', 'Airflow', 'dbt', 'Kafka', 'ETL',
] as const;

export const AI_ML_KEYWORDS = [
  'AI Engineer', 'Machine Learning', 'Generative AI', 'LLM', 'NLP', 'Computer Vision',
  'Deep Learning', 'PyTorch', 'TensorFlow', 'Scikit-learn', 'LangChain', 'RAG', 'MLOps',
  'Data Science', 'OpenAI', 'Hugging Face',
] as const;

export const CLOUD_DEVOPS_KEYWORDS = [
  'AWS', 'Azure', 'GCP', 'Docker', 'Kubernetes', 'Terraform', 'Ansible', 'CI/CD',
  'Jenkins', 'GitLab CI', 'GitHub Actions', 'Linux', 'DevOps', 'SRE', 'Platform Engineer',
  'Systems Engineer', 'SysAdmin', 'Cloud Engineer', 'Prometheus', 'Grafana',
] as const;

export const QA_CYBER_KEYWORDS = [
  'QA', 'Test Automation', 'SDET', 'Playwright', 'Cypress', 'Selenium', 'JUnit', 'Postman',
  'Cybersecurity', 'Security Engineer', 'SOC', 'SIEM', 'Pentesting', 'AppSec', 'DevSecOps', 'ISO 27001',
] as const;

export const SAP_KEYWORDS = [
  'SAP', 'ABAP', 'S/4HANA', 'RAP', 'CAP', 'OData', 'Fiori', 'BTP',
  'SAP HANA', 'FI/CO', 'SuccessFactors', 'Salesforce', 'Dynamics 365', 'ServiceNow', 'ERP',
] as const;

export const ALL_TECH_HIGHLIGHT_KEYWORDS = Array.from(
  new Set<string>([
    ...FULLSTACK_KEYWORDS,
    ...PYTHON_DATA_KEYWORDS,
    ...AI_ML_KEYWORDS,
    ...CLOUD_DEVOPS_KEYWORDS,
    ...QA_CYBER_KEYWORDS,
    ...SAP_KEYWORDS,
    'Go', 'Rust', 'C++', 'Git',
  ]),
);

export const STACK_PRESETS: readonly StackPreset[] = [
  {
    id: 'all-tech',
    label: 'All Tech / Software Roles',
    shortLabel: 'All Tech Roles',
    description: 'Broad software engineering, web & mobile, Python, data analytics, AI/ML, cloud/DevOps, QA, cybersecurity, and ERP roles.',
    keywords: ALL_TECH_HIGHLIGHT_KEYWORDS,
  },
  {
    id: 'fullstack',
    label: 'Software & Web Development',
    shortLabel: 'Software & Web',
    description: 'Software developer, backend, frontend, full-stack, and mobile roles across Java, JavaScript/TypeScript, React, Angular, Vue, Node.js, C#/.NET, PHP, Kotlin, and SQL.',
    keywords: FULLSTACK_KEYWORDS,
  },
  {
    id: 'python-data',
    label: 'Python, Data & Analytics',
    shortLabel: 'Python & Data',
    description: 'Python programming, Data Analyst, Data Engineer, Business Intelligence (BI), SQL, Power BI, Tableau, ETL, Spark, Databricks, and Snowflake roles.',
    keywords: PYTHON_DATA_KEYWORDS,
  },
  {
    id: 'ai-ml',
    label: 'AI & Machine Learning',
    shortLabel: 'AI & ML',
    description: 'AI Engineering, Machine Learning, Generative AI, LLMs, NLP, Computer Vision, PyTorch, TensorFlow, RAG, and MLOps roles.',
    keywords: AI_ML_KEYWORDS,
  },
  {
    id: 'cloud-devops',
    label: 'Cloud, DevOps & Systems',
    shortLabel: 'Cloud & DevOps',
    description: 'Cloud engineering, DevOps, SRE, platform, Linux systems administration, Docker, Kubernetes, AWS, Azure, GCP, Terraform, and CI/CD roles.',
    keywords: CLOUD_DEVOPS_KEYWORDS,
  },
  {
    id: 'qa-cyber',
    label: 'QA Automation & Cybersecurity',
    shortLabel: 'QA & Cyber',
    description: 'QA engineer, test automation, SDET, Playwright, Cypress, Selenium, cybersecurity analyst, SOC, pentesting, and AppSec roles.',
    keywords: QA_CYBER_KEYWORDS,
  },
  {
    id: 'sap',
    label: 'SAP, ERP & Enterprise CRM',
    shortLabel: 'SAP & ERP',
    description: 'SAP S/4HANA, ABAP, RAP, CAP, OData, Fiori/UI5, BTP, functional SAP modules (FI/CO, MM, SD), Salesforce, Microsoft Dynamics 365, and ServiceNow roles.',
    keywords: SAP_KEYWORDS,
  },
  {
    id: 'custom',
    label: 'Custom Profile',
    shortLabel: 'Custom Profile',
    description: 'Define your own keyword tags or sync from your saved developer profile.',
    keywords: [],
  },
];

export const DEFAULT_CUSTOM_TAGS = ['Python', 'Java', 'TypeScript', 'React', 'SQL', 'Docker'] as const;

export function parsePresetId(raw: string | null): StackPresetId {
  if (
    raw === 'sap'
    || raw === 'fullstack'
    || raw === 'python-data'
    || raw === 'ai-ml'
    || raw === 'cloud-devops'
    || raw === 'qa-cyber'
    || raw === 'custom'
    || raw === 'all-tech'
  ) {
    return raw;
  }
  return 'all-tech';
}

export function loadCustomTags(profile: ProfessionalProfile | null): string[] {
  try {
    const raw = window.localStorage.getItem(CUSTOM_STACK_STORAGE_KEY);
    if (raw) {
      const parsed: unknown = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        const clean = parsed
          .filter((item): item is string => typeof item === 'string')
          .map((item) => item.trim())
          .filter((item) => item.length > 0 && item.length <= 40);
        if (clean.length > 0) {
          return [...new Set(clean)].slice(0, 20);
        }
      }
    }
  } catch {
    // Ignore restricted storage and fall back to profile or default tags.
  }
  if (profile && profile.technologies.length > 0) {
    return [...profile.technologies];
  }
  return [...DEFAULT_CUSTOM_TAGS];
}

export function saveCustomTags(tags: string[]): boolean {
  try {
    window.localStorage.setItem(CUSTOM_STACK_STORAGE_KEY, JSON.stringify(tags.slice(0, 20)));
    return true;
  } catch {
    return false;
  }
}
