import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles.css';

function App() {
  return (
    <main className="app-shell">
      <section className="status-panel">
        <p className="eyebrow">TripGenie Enterprise</p>
        <h1>Platform foundation</h1>
        <p>Phase 0 React shell connected to the monorepo scaffold.</p>
      </section>
    </main>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
