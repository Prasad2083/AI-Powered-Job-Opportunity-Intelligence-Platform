export const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';
export function apiError(error, fallback = 'Request failed') {
  const data = error.response?.data;
  if (data?.fieldErrors) {
    const fields = Object.entries(data.fieldErrors).map(([name, message]) => `${name}: ${message}`);
    if (fields.length) return fields.join('; ');
  }
  return data?.message || data?.error || fallback;
}
