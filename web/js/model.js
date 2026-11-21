// Resume data helpers. The shape matches the Android app's Resume (see backend/src/utils/resumeSchema.js).

export const factories = {
  experience: () => ({ company: '', role: '', startDate: '', endDate: '', isCurrent: false, description: '' }),
  education: () => ({ institution: '', degree: '', startYear: '', endYear: '', grade: '', achievements: '' }),
  skills: () => ({ name: '', level: 'Intermediate', category: 'Technical' }),
  projects: () => ({ title: '', techStack: '', description: '', githubLink: '', liveLink: '' }),
  certifications: () => ({ name: '', issuer: '', date: '', credentialId: '', url: '' }),
  languages: () => ({ name: '', proficiency: 'Fluent' }),
};

export function blankData(user) {
  return {
    fullName: (user && user.name) || '', jobTitle: '', phone: '', email: (user && user.email) || '', location: '',
    linkedinUrl: '', githubUrl: '', portfolioUrl: '', summary: '',
    education: [], experience: [], skills: [], projects: [], certifications: [], languages: [],
    achievements: '', templateId: 'Modern Professional', colorSchemeId: 'Blue', fontSize: 'Standard', margins: 'Standard',
    atsScore: 0, atsFeedback: '',
  };
}

/** Makes sure every field exists, so the editor never meets undefined. */
export function normalize(resume) {
  const base = blankData(null);
  const data = { ...base, ...(resume.data || {}) };
  for (const k of Object.keys(factories)) data[k] = Array.isArray(data[k]) ? data[k] : [];
  for (const [k, v] of Object.entries(base)) if (typeof v === 'string' && typeof data[k] !== 'string') data[k] = '';
  return { ...resume, data };
}

export function newRef() {
  return 'w' + (crypto.randomUUID ? crypto.randomUUID().replace(/-/g, '') : String(Date.now()) + Math.random().toString(36).slice(2, 12));
}
