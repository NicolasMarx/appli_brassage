module Pages.Referentials exposing (view)

import Html exposing (..)

view : Html msg
view =
    div []
        [ h2 [] [ text "Référentiels (Aromas, ...)" ]
        , p [] [ text "Affiche les listes via /api/aromas (Query). Étendre au besoin." ]
        ]

