export function downloadBlob(data, filename) {
  const url = URL.createObjectURL(data);
  const anchor = document.createElement('a');
  anchor.href = url; anchor.download = filename;
  document.body.append(anchor); anchor.click(); anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function downloadWordBlob(data, filename) {
  if (!data.type?.startsWith('application/vnd.openxmlformats-officedocument.wordprocessingml.document')) {
    throw new Error('Word export is unavailable in the running backend. Restart JobPulse after pulling the latest changes.');
  }
  downloadBlob(data, filename);
}
