package sistema_organizacion.sistema.usecases.tarea.impl;

import sistema_organizacion.sistema.entities.JefeDeHogar;
import sistema_organizacion.sistema.entities.Usuario;
import sistema_organizacion.sistema.entities.exception.AccesoDenegadoException;
import sistema_organizacion.sistema.entities.exception.TareaNoEncontradaException;
import sistema_organizacion.sistema.ports.outs.TareaOutputPort;
import sistema_organizacion.sistema.ports.outs.UsuarioOutputPort;
import sistema_organizacion.sistema.usecases.tarea.EliminarTareaCommand;
import sistema_organizacion.sistema.usecases.tarea.EliminarTareaUseCase;

public class EliminarTareaInteractor implements EliminarTareaUseCase {

    private final UsuarioOutputPort usuarioOutputPort;
    private final TareaOutputPort tareaOutputPort;

    public EliminarTareaInteractor(UsuarioOutputPort usuarioOutputPort,
                                   TareaOutputPort tareaOutputPort) {
        this.usuarioOutputPort = usuarioOutputPort;
        this.tareaOutputPort = tareaOutputPort;
    }

    @Override
    public void ejecutar(EliminarTareaCommand command) {
        Usuario usuario = usuarioOutputPort
            .buscarPorId(command.getJefeId())
            .orElseThrow(AccesoDenegadoException::new);

        if (!(usuario instanceof JefeDeHogar)) {
            throw new AccesoDenegadoException();
        }

        tareaOutputPort.buscarPorId(command.getTareaId())
            .orElseThrow(() -> new TareaNoEncontradaException(command.getTareaId()));

        tareaOutputPort.eliminar(command.getTareaId());
    }
}
