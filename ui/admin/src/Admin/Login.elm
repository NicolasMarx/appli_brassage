module Admin.Login exposing (Model, Msg(..), init, update, view)

import Browser.Navigation as Nav
import Html exposing (Html, button, div, form, h2, input, label, text)
import Html.Attributes as A
import Html.Events as E
import Http
import Admin.Auth as Auth

-- MODEL
type alias Model =
    { email : String
    , password : String
    , status : Status
    }

type Status
    = Idle
    | Loading
    | Failure String
    | Success Auth.Admin

init : Model
init =
    { email = ""
    , password = ""
    , status = Idle
    }

-- MSG
type Msg
    = EmailChanged String
    | PasswordChanged String
    | Submit
    | GotLogin (Result Http.Error Auth.Admin)

-- UPDATE
update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        EmailChanged s ->
            ( { model | email = s }, Cmd.none )

        PasswordChanged s ->
            ( { model | password = s }, Cmd.none )

        Submit ->
            if String.length model.password < 1 || String.length model.email < 3 then
                ( { model | status = Failure "Email ou mot de passe manquant" }, Cmd.none )
            else
                ( { model | status = Loading }
                , Auth.loginRequest model.email model.password GotLogin
                )

        GotLogin result ->
            case result of
                Ok _ ->
                    -- Redirection client vers /admin après succès
                    ( { model | status = Idle }, Nav.load "/admin" )

                Err httpErr ->
                    let
                        msg_ =
                            case httpErr of
                                Http.BadBody bodyMsg -> "Réponse illisible : " ++ bodyMsg
                                Http.BadStatus _     -> "Identifiants invalides"
                                Http.Timeout         -> "Timeout"
                                Http.NetworkError    -> "Erreur réseau"
                                Http.BadUrl u        -> "URL invalide: " ++ u
                    in
                    ( { model | status = Failure msg_ }, Cmd.none )

-- VIEW
view : Model -> Html Msg
view model =
    div [ A.class "login-page" ]
        [ div [ A.class "card" ]
            [ h2 [] [ text "Connexion admin" ]
            , form [ E.onSubmit Submit ]
                [ label [ A.for "email" ] [ text "Email" ]
                , input [ A.id "email", A.type_ "email", A.placeholder "you@example.com", A.value model.email, E.onInput EmailChanged ] []
                , label [ A.for "password" ] [ text "Mot de passe" ]
                , input [ A.id "password", A.type_ "password", A.placeholder "••••••••", A.value model.password, E.onInput PasswordChanged ] []
                , button [ A.type_ "submit", A.disabled (model.status == Loading) ]
                    [ text (if model.status == Loading then "Connexion…" else "Se connecter") ]
                ]
            , viewStatus model.status
            ]
        ]

viewStatus : Status -> Html msg
viewStatus status =
    case status of
        Idle -> text ""
        Loading -> div [ A.class "info" ] [ text "Connexion en cours…" ]
        Failure err -> div [ A.class "error" ] [ text err ]
        Success _ -> text ""
