## MODIFIED Requirements

### Requirement: Tool Window registration
The plugin SHALL register a Tool Window with id `spek` that appears in the IDE's right sidebar. The Tool Window SHALL display a vertical split pane whose bottom component is always the JCEF webview content (or external browser fallback), and whose top component is the OpenSpec tree navigator, present subject to the project's persisted tree visibility preference. When the tree navigator is hidden, the webview content SHALL occupy the whole Tool Window.

#### Scenario: Tool Window visibility
- **WHEN** a project with OpenSpec content is detected
- **THEN** a "spek" Tool Window icon SHALL appear in the right sidebar
- **AND** clicking the icon SHALL open/reveal the Tool Window with the webview

#### Scenario: Tool Window with the tree navigator visible
- **WHEN** the project's persisted tree visibility preference is "visible", or no preference is stored
- **AND** the spek Tool Window is opened
- **THEN** the Tool Window SHALL display both the tree navigator and the webview, separated by a draggable divider

#### Scenario: Tool Window with the tree navigator hidden
- **WHEN** the project's persisted tree visibility preference is "hidden"
- **AND** the spek Tool Window is opened
- **THEN** the Tool Window SHALL display only the webview, occupying the full Tool Window
- **AND** the tree navigator SHALL be reachable again via the Tool Window's visibility toggle
