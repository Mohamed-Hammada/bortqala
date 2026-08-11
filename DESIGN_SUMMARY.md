# User Creation UX — Before vs After

| Area | Current | Redesigned |
|---|---|---|
| Account data | Mixed into one very long modal | First compact panel |
| Role selection | Technical code, sensitivity, kind, page/action metadata | Name + business description |
| Guided access | Separate competing role-selection model | Removed from normal create/edit flow |
| Find by page | Always visible | Removed from normal flow |
| Menu permissions | Always visible and manually coordinated with roles | Auto-derived for new users; manual override collapsed |
| Effective access preview | Always visible | Available under Advanced Access |
| Existing custom menus | Could be manually edited | Explicitly preserved in edit mode |
| Backend validation | Present | Preserved |
| Sensitive access acknowledgment | Present | Preserved, shown only when relevant |
| Mobile complexity | Very long dense modal | Fewer default sections; 1-column responsive cards |

## UX principle

An administrator creating a normal user should answer two questions: **Who is this person?** and **What business role do they have?** Detailed menu-level authorization is an exception workflow, not the default workflow.
