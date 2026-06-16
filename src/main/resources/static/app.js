import React, { useEffect, useState } from "https://esm.sh/react@18.3.1";
import { createRoot } from "https://esm.sh/react-dom@18.3.1/client";
import htm from "https://esm.sh/htm@3.1.1";

const html = htm.bind(React.createElement);

const emptyForm = {
    title: "",
    content: "",
};

function formatDate(value) {
    return new Intl.DateTimeFormat("ru-RU", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(new Date(value));
}

function App() {
    const [notes, setNotes] = useState([]);
    const [selectedId, setSelectedId] = useState(null);
    const [form, setForm] = useState(emptyForm);
    const [query, setQuery] = useState("");
    const [status, setStatus] = useState("Загружаю заметки...");
    const [isSaving, setIsSaving] = useState(false);

    const loadNotes = async (preferredId) => {
        const response = await fetch("/api/notes");
        const data = await response.json();
        setNotes(data);

        if (data.length === 0) {
            setSelectedId(null);
            setForm(emptyForm);
            return;
        }

        const nextSelected = preferredId && data.some((note) => note.id === preferredId)
            ? preferredId
            : data[0].id;
        const current = data.find((note) => note.id === nextSelected) ?? data[0];

        setSelectedId(current.id);
        setForm({
            title: current.title,
            content: current.content,
        });
    };

    useEffect(() => {
        loadNotes()
            .then(() => setStatus("Готово"))
            .catch(() => setStatus("Не удалось загрузить заметки"));
    }, []);

    const filteredNotes = notes.filter((note) => {
        const haystack = `${note.title} ${note.content}`.toLowerCase();
        return haystack.includes(query.toLowerCase());
    });

    const selectedNote = notes.find((note) => note.id === selectedId) ?? null;

    const startCreate = () => {
        setSelectedId(null);
        setForm(emptyForm);
        setStatus("Новая заметка");
    };

    const selectNote = (note) => {
        setSelectedId(note.id);
        setForm({
            title: note.title,
            content: note.content,
        });
        setStatus(`Редактирование: ${note.title}`);
    };

    const saveNote = async (event) => {
        event.preventDefault();

        if (!form.title.trim()) {
            setStatus("Укажите заголовок заметки");
            return;
        }

        setIsSaving(true);

        const method = selectedId ? "PUT" : "POST";
        const url = selectedId ? `/api/notes/${selectedId}` : "/api/notes";

        try {
            const response = await fetch(url, {
                method,
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(form),
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || "Ошибка сохранения");
            }

            const saved = await response.json();
            await loadNotes(saved.id);
            setStatus(selectedId ? "Заметка обновлена" : "Заметка создана");
        } catch (error) {
            setStatus(error.message);
        } finally {
            setIsSaving(false);
        }
    };

    const deleteNote = async () => {
        if (!selectedId) {
            return;
        }

        const approved = window.confirm("Удалить текущую заметку?");
        if (!approved) {
            return;
        }

        try {
            const response = await fetch(`/api/notes/${selectedId}`, {
                method: "DELETE",
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || "Ошибка удаления");
            }

            await loadNotes();
            setStatus("Заметка удалена");
        } catch (error) {
            setStatus(error.message);
        }
    };

    return html`
        <main className="page">
            <section className="hero">
                <div>
                    <p className="eyebrow">Simple notes stack</p>
                    <h1>Заметки</h1>
                    <p className="subtitle">
                        Создавайте, редактируйте и удаляйте заметки в одном окне.
                    </p>
                </div>
                <button className="primary ghost" onClick=${startCreate}>Новая заметка</button>
            </section>

            <section className="workspace">
                <aside className="sidebar">
                    <div className="panel-header">
                        <h2>Все заметки</h2>
                        <span>${notes.length}</span>
                    </div>

                    <label className="search">
                        <span>Поиск</span>
                        <input
                            type="search"
                            value=${query}
                            onInput=${(event) => setQuery(event.target.value)}
                            placeholder="Заголовок или текст"
                        />
                    </label>

                    <div className="notes-list">
                        ${filteredNotes.length === 0 && html`
                            <div className="empty-state">
                                <p>Ничего не найдено.</p>
                                <button className="link-button" onClick=${() => setQuery("")}>Сбросить поиск</button>
                            </div>
                        `}

                        ${filteredNotes.map((note) => html`
                            <button
                                key=${note.id}
                                className=${`note-card ${note.id === selectedId ? "active" : ""}`}
                                onClick=${() => selectNote(note)}
                            >
                                <strong>${note.title}</strong>
                                <p>${note.content || "Без текста"}</p>
                                <time>${formatDate(note.updatedAt)}</time>
                            </button>
                        `)}
                    </div>
                </aside>

                <section className="editor">
                    <div className="panel-header">
                        <div>
                            <h2>${selectedNote ? "Редактор" : "Новая заметка"}</h2>
                            <p className="muted">${status}</p>
                        </div>
                        ${selectedNote && html`
                            <button className="danger" onClick=${deleteNote}>Удалить</button>
                        `}
                    </div>

                    <form className="editor-form" onSubmit=${saveNote}>
                        <label>
                            <span>Заголовок</span>
                            <input
                                type="text"
                                value=${form.title}
                                onInput=${(event) => setForm({ ...form, title: event.target.value })}
                                placeholder="Например, список дел"
                            />
                        </label>

                        <label>
                            <span>Текст</span>
                            <textarea
                                rows="12"
                                value=${form.content}
                                onInput=${(event) => setForm({ ...form, content: event.target.value })}
                                placeholder="Запишите мысль, задачу или черновик"
                            ></textarea>
                        </label>

                        <div className="editor-actions">
                            <button className="primary" type="submit" disabled=${isSaving}>
                                ${isSaving ? "Сохраняю..." : selectedNote ? "Сохранить изменения" : "Создать заметку"}
                            </button>
                            <button className="secondary" type="button" onClick=${startCreate}>Очистить</button>
                        </div>
                    </form>
                </section>
            </section>
        </main>
    `;
}

createRoot(document.getElementById("root")).render(html`<${App} />`);
