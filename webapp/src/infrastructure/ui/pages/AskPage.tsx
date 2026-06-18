import React, { useState, useRef, useEffect } from "react";
import {
  Container,
  Box,
  Typography,
  TextField,
  Button,
  Paper,
  CircularProgress,
  Alert,
  Chip,
  Link as MuiLink,
  IconButton,
  Divider,
  Stack
} from "@mui/material";
import { Send, DeleteSweep, AutoAwesome, OpenInNew } from "@mui/icons-material";
import { useAppStore } from "../../../application/state/store";
import { ConversationEntry } from "../../../domain/models/QA";

// Only allow http(s) source links so a stored `javascript:`/`data:` URL can't execute when clicked.
const isHttpUrl = (url: string): boolean => /^https?:\/\//i.test(url);

const ConversationEntryCard: React.FC<{ entry: ConversationEntry }> = ({ entry }) => (
  <Box>
    {/* Question */}
    <Box display="flex" justifyContent="flex-end" mb={1}>
      <Paper
        elevation={0}
        sx={{
          px: 2,
          py: 1.5,
          maxWidth: "75%",
          bgcolor: "primary.main",
          color: "primary.contrastText",
          borderRadius: "16px 16px 4px 16px"
        }}
      >
        <Typography variant="body1">{entry.question}</Typography>
      </Paper>
    </Box>

    {/* Answer */}
    <Box display="flex" justifyContent="flex-start" mb={1}>
      <Paper
        elevation={1}
        sx={{
          px: 2,
          py: 1.5,
          maxWidth: "85%",
          borderRadius: "16px 16px 16px 4px"
        }}
      >
        <Box display="flex" alignItems="center" gap={0.5} mb={1}>
          <AutoAwesome sx={{ fontSize: 14, color: "primary.main" }} />
          <Typography variant="caption" color="primary.main" fontWeight={600}>
            LinkLift AI
          </Typography>
        </Box>
        <Typography variant="body1" sx={{ whiteSpace: "pre-wrap" }}>
          {entry.answer}
        </Typography>

        {entry.sources.length > 0 && (
          <Box mt={2}>
            <Divider sx={{ mb: 1 }} />
            <Typography variant="caption" color="text.secondary" display="block" mb={1}>
              Sources
            </Typography>
            <Stack spacing={0.5}>
              {entry.sources.map((source) => (
                <Box key={source.linkId} display="flex" alignItems="center" gap={0.5}>
                  <MuiLink
                    href={isHttpUrl(source.url) ? source.url : undefined}
                    target="_blank"
                    rel="noopener noreferrer"
                    variant="caption"
                    underline="hover"
                    sx={{ display: "flex", alignItems: "center", gap: 0.25 }}
                  >
                    {source.title}
                    <OpenInNew sx={{ fontSize: 10 }} />
                  </MuiLink>
                </Box>
              ))}
            </Stack>
          </Box>
        )}
      </Paper>
    </Box>

    <Typography variant="caption" color="text.disabled" display="block" textAlign="center" mb={2}>
      {new Date(entry.timestamp).toLocaleTimeString()}
    </Typography>
  </Box>
);

export const AskPage: React.FC = () => {
  const { conversation, isAsking, askError, ask, clearConversation } = useAppStore();
  const [question, setQuestion] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [conversation]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const q = question.trim();
    if (!q || isAsking) return;
    setQuestion("");
    await ask(q);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e as unknown as React.FormEvent);
    }
  };

  return (
    <Container maxWidth="md">
      <Box py={4} display="flex" flexDirection="column" height="calc(100vh - 120px)">
        {/* Header */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Box>
            <Typography variant="h4" component="h1" fontWeight={600}>
              Ask Your Links
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Ask questions about your saved content
            </Typography>
          </Box>
          {conversation.length > 0 && (
            <IconButton onClick={clearConversation} title="Clear conversation" color="default">
              <DeleteSweep />
            </IconButton>
          )}
        </Box>

        {/* Conversation area */}
        <Box flex={1} overflow="auto" mb={2}>
          {conversation.length === 0 && !isAsking && (
            <Paper sx={{ p: 4, textAlign: "center", bgcolor: "grey.50", borderRadius: 3 }} elevation={0}>
              <AutoAwesome sx={{ fontSize: 40, color: "primary.main", mb: 1 }} />
              <Typography variant="h6" gutterBottom>
                Ask anything about your saved links
              </Typography>
              <Typography variant="body2" color="text.secondary">
                I'll search through your saved content and synthesize an answer with source citations.
              </Typography>
              <Box mt={2} display="flex" gap={1} flexWrap="wrap" justifyContent="center">
                {["What did I save about React hooks?", "Summarize my AI articles", "What are best practices for TypeScript?"].map((s) => (
                  <Chip key={s} label={s} variant="outlined" size="small" onClick={() => setQuestion(s)} sx={{ cursor: "pointer" }} />
                ))}
              </Box>
            </Paper>
          )}

          {conversation.map((entry) => (
            <ConversationEntryCard key={entry.id} entry={entry} />
          ))}

          {isAsking && (
            <Box display="flex" justifyContent="flex-start" mb={1}>
              <Paper elevation={1} sx={{ px: 2, py: 1.5, borderRadius: "16px 16px 16px 4px" }}>
                <Box display="flex" alignItems="center" gap={1}>
                  <CircularProgress size={16} />
                  <Typography variant="body2" color="text.secondary">
                    Searching your links...
                  </Typography>
                </Box>
              </Paper>
            </Box>
          )}

          {askError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {askError}
            </Alert>
          )}

          <div ref={bottomRef} />
        </Box>

        {/* Input area */}
        <Paper elevation={2} sx={{ p: 1.5, borderRadius: 3 }}>
          <Box component="form" onSubmit={handleSubmit} display="flex" gap={1} alignItems="flex-end">
            <TextField
              fullWidth
              multiline
              maxRows={4}
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask a question about your saved links..."
              variant="outlined"
              size="small"
              disabled={isAsking}
              sx={{ "& .MuiOutlinedInput-root": { borderRadius: 2 } }}
            />
            <Button type="submit" variant="contained" disabled={!question.trim() || isAsking} sx={{ borderRadius: 2, minWidth: 48, px: 1.5, py: 1 }}>
              <Send />
            </Button>
          </Box>
          <Typography variant="caption" color="text.disabled" sx={{ mt: 0.5, display: "block", pl: 1 }}>
            Press Enter to send, Shift+Enter for new line
          </Typography>
        </Paper>
      </Box>
    </Container>
  );
};
