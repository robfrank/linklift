import React, { useState, useEffect } from "react";
import { Box, TextField, Autocomplete, Typography, CircularProgress, IconButton, Tooltip, Collapse } from "@mui/material";
import { LocalOffer, Add, ExpandMore, ExpandLess } from "@mui/icons-material";
import { Tag } from "../../../../domain/models/Tag";
import { useAppStore } from "../../../../application/state/store";
import { TagChips } from "./TagChips";

interface LinkTagManagerProps {
  linkId: string;
  compact?: boolean;
}

export const LinkTagManager: React.FC<LinkTagManagerProps> = ({ linkId, compact = false }) => {
  const { tags, fetchTags, linkTags, fetchTagsForLink, createTag, addTagToLink, removeTagFromLink } = useAppStore();
  const [expanded, setExpanded] = useState(!compact);
  const [inputValue, setInputValue] = useState("");
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    fetchTagsForLink(linkId);
  }, [linkId]);

  useEffect(() => {
    if (expanded && tags.length === 0) {
      fetchTags();
    }
  }, [expanded]);

  const currentTags: Tag[] = linkTags[linkId] || [];
  const availableTags = tags.filter((t: Tag) => !currentTags.some((ct: Tag) => ct.id === t.id));

  const handleAddTag = async (tagName: string) => {
    if (!tagName.trim()) return;
    setAdding(true);
    try {
      // Find or create the tag
      const existingTag = tags.find((t) => t.name === tagName.toLowerCase().trim());
      let tag: Tag | null = existingTag || null;
      if (!tag) {
        tag = await createTag(tagName.trim());
      }
      if (tag) {
        await addTagToLink(linkId, tag.id);
        setInputValue("");
      }
    } finally {
      setAdding(false);
    }
  };

  const handleRemoveTag = async (tagId: string) => {
    await removeTagFromLink(linkId, tagId);
  };

  if (compact) {
    return (
      <Box>
        <Box display="flex" alignItems="center" gap={0.5}>
          <TagChips tags={currentTags} onRemove={handleRemoveTag} />
          <Tooltip title="Manage tags">
            <IconButton size="small" onClick={() => setExpanded(!expanded)} sx={{ p: 0.5 }}>
              <LocalOffer sx={{ fontSize: 14 }} />
              {expanded ? <ExpandLess sx={{ fontSize: 12 }} /> : <ExpandMore sx={{ fontSize: 12 }} />}
            </IconButton>
          </Tooltip>
        </Box>
        <Collapse in={expanded}>
          <Box mt={1}>
            <TagInput availableTags={availableTags} inputValue={inputValue} onInputChange={setInputValue} onAdd={handleAddTag} adding={adding} />
          </Box>
        </Collapse>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: "flex", alignItems: "center", gap: 0.5, mb: 1 }}>
        <LocalOffer sx={{ fontSize: 12 }} /> Tags
      </Typography>
      <Box display="flex" flexWrap="wrap" gap={0.5} mb={1}>
        <TagChips tags={currentTags} onRemove={handleRemoveTag} />
      </Box>
      <TagInput availableTags={availableTags} inputValue={inputValue} onInputChange={setInputValue} onAdd={handleAddTag} adding={adding} />
    </Box>
  );
};

interface TagInputProps {
  availableTags: Tag[];
  inputValue: string;
  onInputChange: (v: string) => void;
  onAdd: (name: string) => void;
  adding: boolean;
}

const TagInput: React.FC<TagInputProps> = ({ availableTags, inputValue, onInputChange, onAdd, adding }) => {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && inputValue.trim()) {
      e.preventDefault();
      onAdd(inputValue.trim());
    }
  };

  return (
    <Box display="flex" alignItems="center" gap={0.5}>
      <Autocomplete
        freeSolo
        size="small"
        options={availableTags.map((t) => t.name)}
        inputValue={inputValue}
        onInputChange={(_, value) => onInputChange(value)}
        onChange={(_, value) => {
          if (value) onAdd(value);
        }}
        renderInput={(params) => (
          <TextField
            {...params}
            placeholder="Add tag..."
            onKeyDown={handleKeyDown}
            sx={{ minWidth: 140 }}
            InputProps={{
              ...params.InputProps,
              sx: { fontSize: "0.75rem", height: 28 }
            }}
          />
        )}
        sx={{ flexGrow: 1 }}
      />
      {adding ? (
        <CircularProgress size={16} />
      ) : (
        <Tooltip title="Add tag (or press Enter)">
          <IconButton size="small" onClick={() => inputValue.trim() && onAdd(inputValue.trim())} disabled={!inputValue.trim()}>
            <Add sx={{ fontSize: 16 }} />
          </IconButton>
        </Tooltip>
      )}
    </Box>
  );
};
