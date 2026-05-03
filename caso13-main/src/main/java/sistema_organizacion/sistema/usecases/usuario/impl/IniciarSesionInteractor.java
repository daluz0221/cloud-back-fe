package sistema_organizacion.sistema.usecases.usuario.impl;

import sistema_organizacion.sistema.entities.MiembroHogar;
import sistema_organizacion.sistema.entities.Usuario;
import sistema_organizacion.sistema.entities.exception.CamposObligatoriosException;
import sistema_organizacion.sistema.entities.exception.CredencialesInvalidasException;
import sistema_organizacion.sistema.entities.exception.UsuarioNoRegistradoException;
import sistema_organizacion.sistema.ports.outs.UsuarioOutputPort;
import sistema_organizacion.sistema.usecases.usuario.IniciarSesionCommand;
import sistema_organizacion.sistema.usecases.usuario.IniciarSesionUseCase;
import sistema_organizacion.sistema.usecases.usuario.SesionResult;

public class IniciarSesionInteractor implements IniciarSesionUseCase {

    private final UsuarioOutputPort usuarioOutputPort;

    public IniciarSesionInteractor(UsuarioOutputPort usuarioOutputPort) {
        this.usuarioOutputPort = usuarioOutputPort;
    }

    @Override
    public SesionResult ejecutar(IniciarSesionCommand command) {

        // CA-04-A HU-10: campos obligatorios
        if (command.getCorreo() == null || command.getCorreo().isBlank()
                || command.getContrasena() == null
                || command.getContrasena().isBlank()) {
            throw new CamposObligatoriosException();
        }

        // CA-03-B HU-10: usuario no registrado
        Usuario usuario = usuarioOutputPort
            .buscarPorCorreo(command.getCorreo())
            .orElseThrow(UsuarioNoRegistradoException::new);

        // CA-03-A HU-10: contraseña incorrecta
        if (!usuario.getContrasena().equals(command.getContrasena())) {
            throw new CredencialesInvalidasException("Credenciales incorrectas");
        }

        // CA-02 HU-10: detectar si el miembro tiene grupo
        boolean tieneGrupo = false;
        if (usuario instanceof MiembroHogar miembro) {
            tieneGrupo = miembro.tieneGrupo();
        } else {
            tieneGrupo = true; // el admin siempre tiene acceso directo
        }

        return new SesionResult(usuario, tieneGrupo);
    }
}