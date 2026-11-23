# Changelog

## 0.2.19+21

- Add explicit reusable single-choice and option rows, and align toggle text hierarchy with shared settings rows.
- Keep decayed thumbnail-strip flinging with centered settling and no edge rebound; direct main-image swipes still advance one image at a time.

## 0.2.18+20

- Disable edge overscroll on the thumbnail strip so the last thumbnail settles without a rebound; retain decayed fling and centered snapping.

## 0.2.17+19

- Add decayed snapping to the thumbnail strip: it coasts after release and settles with a thumbnail centered. Main-image gestures still page one image at a time.

## 0.2.16+18

- Free scrolling fixed the immediate-stop feel but could leave the strip between thumbnails; superseded by centered snapping in 0.2.17.

## 0.2.15+17

- First scope correction; the strip still snapped to pages and was replaced by free scrolling in 0.2.16.

## 0.2.14+16

- First attempt at slower release snapping; superseded because it also changed main-image paging.

## 0.2.13+15

- Let image and thumbnail pagers continue by velocity after release, snapping at most two pages per fling.

## 0.2.12+14

- Synchronize the gallery thumbnail strip continuously with the main image swipe in both directions.

## 0.2.11+13

- Keep the gallery thumbnail strip synchronized with image paging during a partial swipe.

## 0.2.10+12

- Match the gallery's non-glass top controls to Review, including the dark circular surface and 22dp icons.


## 0.2.9+11

- Keep the gallery thumbnail strip at its original height while dragging across a video, then reposition it after the drag finishes.
- Render the non-glass gallery controls with the same icon font and glyphs used by RedNote.


## 0.2.8+10

Synced the UI source code for Lucent UI 0.2.8+10.

- Keep the gallery thumbnail strip in place during a page swipe, then move it above video controls after the pager settles.
- Keep photos and video in one gallery pager with matching thumbnails and in-gallery playback.
- Match the no-glass gallery controls to RedNote's plain circular style and retain the timeline media examples.
- Center double-tap zoom when a tap lands in a black letterbox area; taps on the rendered image still zoom around their position.


## 0.2.7+9

Synced the UI source code for Lucent UI 0.2.7+9.

- Put mixed photos and video in one gallery pager, with video playback and matching thumbnails.
- Matched the non-glass gallery top controls directly to RedNote's plain controls.

## 0.2.6+8

Synced the UI source code for Lucent UI 0.2.6+8.

- Added a mixed photo-and-video timeline post; photo tiles open the gallery and the video tile opens the player.

## 0.2.5+7

Synced the UI source code for Lucent UI 0.2.5+7.

- Matched the no-glass gallery back icon to the rounded media control style.
- Added distinct timeline examples for text, video, image counts 1–9 and 12, including long, GIF, HDR and overflow states.


## 0.2.4+6

Synced the UI source code for Lucent UI 0.2.4+6.

- Centered double-tap zoom when the gesture lands in the gallery's black letterbox area; taps on the rendered image still zoom around their position.
- Reused the shared gallery implementation in the demo and added geometry regression coverage.
## 0.2.3+5

Synced the UI source code for Lucent UI 0.2.3+5.

- Stabilized the gallery thumbnail strip and video controls in both orientations.
- Restored video haptics and added an in-app predictive back setting.

## 0.2.2+4

Synced the UI source code for Lucent UI 0.2.2+4.

- Fixed timeline navigation, local post composition, and per-post actions.
- Connected settings, saved posts, history, search, and documentation to their intended destinations.

## 0.2.1+3

Synced the UI source code for Lucent UI 0.2.1+3.

- Fixed source-image expand and return transitions across the feed, article and image catalog, including rapid reopening.
- Added a real Ultra HDR sample with matching feed, gallery and thumbnail badges and gain-map-preserving image display.
- Fixed post action menus, independent persistent bookmarks, clipboard copy and native sharing.

## 0.2.0+2

Synced the UI source code for Lucent UI 0.2.0+2.

- Aligned themes, settings, navigation, glass effects, and gallery interactions with Review.
- Retained encrypted backups and functional settings in the demo.
- Added source references and visual comparisons to the design specification.

## 0.1.0+1

Initial local design-system demo. Its visual layout has been superseded by 0.2.0.
