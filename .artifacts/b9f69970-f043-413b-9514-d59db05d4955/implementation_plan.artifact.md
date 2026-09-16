# UI Modernization Plan for Spelost

This plan aims to make the application interface "smoother and more beautiful" by adopting Material 3 principles, refining the color palette, and improving the layout structure.

## Proposed Changes

### [Theme & Colors]

#### [MODIFY] [colors.xml](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/res/values/colors.xml)
- Add secondary and tertiary colors for more depth.
- Introduce subtle surface variants for cards and backgrounds.

#### [MODIFY] [themes.xml](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/res/values/themes.xml)
- Update parent theme to `Theme.Material3.DayNight.NoActionBar`.
- Configure Material 3 color slots (primary, secondary, surface, etc.).

### [Navigation & Main Layout]

#### [MODIFY] [activity_main.xml](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/res/layout/activity_main.xml)
- Replace standard `BottomNavigationView` with a `BottomAppBar` containing a centered FAB cradle.
- Use `BottomNavigationView` inside the `BottomAppBar` or as a standalone M3 `NavigationBar`.
- Add a "cutout" effect for the FAB for a smoother look.

### [UI Components]

#### [MODIFY] [bg_card.xml](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/res/drawable/bg_card.xml)
- Use subtle gradients instead of solid colors.
- Increase corner radius for a softer look.

#### [MODIFY] [bg_input.xml](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/res/drawable/bg_input.xml)
- Improve the visual state of input fields (focus colors, padding).

#### [MODIFY] [fragment_home.xml](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/res/layout/fragment_home.xml)
- Improve spacing and typography hierarchy.
- Refine the weather card with better icons and a more "glassmorphic" or layered design.

### [Animations]

#### [NEW] [slide_in_right.xml](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/res/anim/slide_in_right.xml)
#### [NEW] [slide_out_left.xml](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/res/anim/slide_out_left.xml)
- Add standard fragment transition animations for smoother navigation.

#### [MODIFY] [MainActivity.java](file:///Users/anuarkenzhibayev/Desktop/agro_life/Spelost/app/src/main/java/kz/spelost/agroapp/MainActivity.java)
- Apply the new animations in `showFragment`.

## Verification Plan

### Manual Verification
- Deploy the app to the device and verify:
    - The new navigation bar with FAB cradle looks smooth.
    - Fragment transitions are fluid.
    - The Home screen layout feels more modern and balanced.
    - Colors are consistent and pleasing.
