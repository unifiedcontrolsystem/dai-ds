#/usr/bin/env bash
_ucs_completions()
{

    local cur prev commands subcommands i
    cur=${COMP_WORDS[COMP_CWORD]}
    prev=${COMP_WORDS[COMP_CWORD-1]}
    commands="group view"
    subcommands=(
                 "add remove get list"
                 "system-info event env inventory-history replacement-history inventory-info state network-config snapshot-info snapshot-getref jobpower"
                 )

    case ${COMP_CWORD} in
        1)
            COMPREPLY=($(compgen -W "${commands}" -- ${cur}))
            ;;
        2)
            i=0
            for val in ${commands}; do
                if [ ${prev} == ${val} ]
                then
                    COMPREPLY=($(compgen -W "${subcommands[${i}]}" -- ${cur}))
                    break
                else
                    i=${i}+1
                fi
            done
            ;;
        *)
            COMPREPLY=()
            ;;
    esac

}

complete -F _ucs_completions ucs
