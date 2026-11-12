import { it, expect } from 'vitest';
import { downloadWordBlob } from './download';

it('refuses an old-backend TXT response instead of mislabelling it as a Word file', () => {
  const oldResponse = new Blob(['An existing text-only export'], { type: 'text/plain;charset=utf-8' });
  expect(() => downloadWordBlob(oldResponse, 'resume.docx')).toThrow('Restart JobPulse');
});
