import React from 'react';
import './HomePage.css';

export function Arrow() {
  return <svg className="jp-arrow" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M6 18 18 6M6 6h12v12" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" /></svg>;
}

export function Brand({ href = '#', children }) {
  return <a className="jp-brand" href={href} aria-label="JobPulse home">
    <span className="jp-brand-symbol" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M4 17h6l4-9 5 17 4-8h5" stroke="currentColor" strokeWidth="2.3" strokeLinecap="round" strokeLinejoin="round" /></svg></span>
    <span>JobPulse<span className="jp-brand-period">.</span></span>{children}
  </a>;
}

const steps = [
  ['01', 'Start with your story', 'Upload your resume and review the extracted text. Your original document stays available, unchanged.'],
  ['02', 'Find your alignment', 'Discover opportunities and understand how your skills, experience and preferences align with each role.'],
  ['03', 'Prepare with intention', 'Create a draft grounded in your original experience. Edit, review and save a separate version.'],
  ['04', 'Keep moving forward', 'Complete applications on employer websites, then record your progress in one place.'],
];

export default function HomePage() {
  return <div className="jp-public jp-home">
    <a className="jp-skip-link" href="#home-content">Skip to content</a>
    <header className="jp-site-header">
      <Brand />
      <nav className="jp-main-nav" aria-label="Main navigation">
        <a href="#features">Why JobPulse</a><a href="#workflow">How it works</a><a href="#your-control">Your control</a>
      </nav>
      <div className="jp-header-actions"><a className="jp-login-link" href="#login">Log in</a><a className="jp-button jp-button-small" href="#signup">Sign up <Arrow /></a></div>
    </header>
    <main id="home-content" tabIndex={-1}>
      <section className="jp-hero" aria-labelledby="public-page-title">
        <div className="jp-hero-copy">
          <p className="jp-eyebrow"><span className="jp-live-dot" aria-hidden="true" /> A more considered career move</p>
          <h1 id="public-page-title" tabIndex={-1}>Your experience.<br />Your next <em>opportunity.</em></h1>
          <p className="jp-hero-description">Bring clarity to your job search. Discover relevant roles, understand your fit, and prepare thoughtful applications—with your experience at the centre.</p>
          <div className="jp-hero-actions"><a className="jp-button" href="#signup">Create your account <Arrow /></a><a className="jp-text-link" href="#workflow">Explore the workflow <span aria-hidden="true">↓</span></a></div>
          <p className="jp-hero-assurance"><span aria-hidden="true">✓</span> Original resume preserved <span className="jp-assurance-divider" aria-hidden="true">/</span> You review every draft</p>
        </div>
        <div className="jp-workspace-visual" aria-label="Illustrative workflow preview, not account activity">
          <div className="jp-orbit jp-orbit-one" aria-hidden="true" /><div className="jp-orbit jp-orbit-two" aria-hidden="true" />
          <div className="jp-preview">
            <div className="jp-preview-top"><span className="jp-preview-mark" aria-hidden="true">jp.</span><span>THE OPPORTUNITY WORKSPACE</span><span className="jp-preview-dots" aria-hidden="true">•••</span></div>
            <p className="jp-preview-kicker">A little structure. A clearer direction.</p>
            <h2>From possibility<br />to <em>progress.</em></h2>
            <div className="jp-preview-path">
              <div><span className="jp-path-number">01</span><p><strong>Your experience</strong><small>One original. Room to evolve.</small></p><Arrow /></div>
              <div><span className="jp-path-number">02</span><p><strong>Opportunity alignment</strong><small>Understand the evidence behind a match.</small></p><Arrow /></div>
              <div><span className="jp-path-number">03</span><p><strong>A draft you can stand behind</strong><small>Review. Refine. Make it yours.</small></p><Arrow /></div>
            </div>
            <div className="jp-preview-bottom"><span className="jp-live-dot" aria-hidden="true" /> Thoughtful preparation. Human decisions.</div>
          </div>
          <p className="jp-visual-caption">WORKFLOW PREVIEW <span aria-hidden="true">—</span> ILLUSTRATIVE ONLY</p>
          <div className="jp-floating-note"><span aria-hidden="true">✳︎</span><p>Your next chapter.<br /><strong>Built on your real story.</strong></p></div>
        </div>
      </section>
      <section id="features" className="jp-features jp-section" aria-labelledby="features-title">
        <div className="jp-section-heading"><p className="jp-eyebrow">Purpose in every step</p><h2 id="features-title">Less scattered.<br /><em>More intentional.</em></h2><p>A connected workspace for the decisions that matter—from finding a role to preparing your next application.</p></div>
        <div className="jp-feature-list">
          <article><span className="jp-feature-index">01 / DISCOVER</span><h3>Find opportunities with direction.</h3><p>Explore jobs from public employer listings or add a description yourself. Bring your preferred roles and locations into the search.</p></article>
          <article><span className="jp-feature-index">02 / UNDERSTAND</span><h3>See the reasoning behind the match.</h3><p>Review skill alignment, experience signals and location relevance. Missing evidence is a prompt for review, never a verdict on your potential.</p></article>
          <article><span className="jp-feature-index">03 / PREPARE</span><h3>Put your genuine experience first.</h3><p>AI-assisted preparation highlights relevant excerpts from your resume. You edit the wording, check the facts and decide what to use.</p></article>
        </div>
      </section>
      <section id="workflow" className="jp-workflow jp-section" aria-labelledby="workflow-title">
        <div className="jp-workflow-heading"><p className="jp-eyebrow">A clear way forward</p><h2 id="workflow-title">One workflow.<br /><em>Your next chapter.</em></h2><a className="jp-text-link" href="#signup">Get started <Arrow /></a></div>
        <ol className="jp-steps">{steps.map(([number, title, description]) => <li key={number}><span className="jp-step-number" aria-hidden="true">{number}</span><div><h3>{title}</h3><p>{description}</p></div></li>)}</ol>
      </section>
      <section id="your-control" className="jp-control jp-section" aria-labelledby="control-title">
        <div><p className="jp-eyebrow">Assistance, with accountability</p><h2 id="control-title">Technology supports you.<br /><em>You stay in control.</em></h2></div>
        <div className="jp-control-points"><article><h3>Your original stays yours.</h3><p>Keep the uploaded resume intact while working on separate, saved drafts.</p></article><article><h3>Every claim deserves a review.</h3><p>Check AI-assisted drafts against your experience before approving or downloading them.</p></article><article><h3>Progress is recorded honestly.</h3><p>Applications are completed on employer websites. JobPulse tracks the statuses you report; automatic submission is not currently enabled.</p></article></div>
      </section>
      <section className="jp-final-cta" aria-labelledby="get-started-title"><p className="jp-eyebrow">Make room for what comes next</p><h2 id="get-started-title">A clearer search starts<br />with <em>your story.</em></h2><a className="jp-button" href="#signup">Create your account <Arrow /></a><p>Already have an account? <a href="#login">Log in</a></p></section>
    </main>
    <footer className="jp-footer"><Brand /><p>Discover with clarity. Prepare with confidence.</p><a href="#login">Go to your workspace <Arrow /></a></footer>
  </div>;
}
