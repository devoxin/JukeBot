package me.devoxin.jukebot.tests

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

object PlaylistSerialization {
    private val manager = DefaultAudioPlayerManager().apply {
        registerSourceManager(YoutubeAudioSourceManager())
    }

    private const val playlist = "AAAYeAAMUGhvbmt5IFBoYXZzAAAAIQCwUUFBQWZ3SUFFa1ZPUkMxVElDMGdRblZ6YzBsMFJHOTNiZ0FUUVNCWUlGTWdWQ0JJSUZnZ1ZDQkpJRU1nTGdBQUFBQUFBZHlRQUF0Q1FVbGFSbEp2VmtGRFNRQUJBQ3RvZEhSd2N6b3ZMM2QzZHk1NWIzVjBkV0psTG1OdmJTOTNZWFJqYUQ5MlBVSkJTVnBHVW05V1FVTkpBQWQ1YjNWMGRXSmxBQUFBQUFBQUFBQT0AuFFBQUFoQUlBRjFaSlRrTkRSU0F0SUU5eVlXNW5aU0JEU01PRVUwVlNBQk5CSUZnZ1V5QlVJRWdnV0NCVUlFa2dReUF1QUFBQUFBQUIvN2dBQzB0eVJrTkxkVTVSVVc5akFBRUFLMmgwZEhCek9pOHZkM2QzTG5sdmRYUjFZbVV1WTI5dEwzZGhkR05vUDNZOVMzSkdRMHQxVGxGUmIyTUFCM2x2ZFhSMVltVUFBQUFBQUFBQUFBPT0AxFFBQUFqUUlBSU9PQ3ZPT0RyWE5vYVhKMGIydHBlV0VnTFNCRVJVRlVTQ0JKVXlCT1JVRlNBQk5CSUZnZ1V5QlVJRWdnV0NCVUlFa2dReUF1QUFBQUFBQUNoSWdBQ3pGeFN6bGZTRlZoZEdSVkFBRUFLMmgwZEhCek9pOHZkM2QzTG5sdmRYUjFZbVV1WTI5dEwzZGhkR05vUDNZOU1YRkxPVjlJVldGMFpGVUFCM2x2ZFhSMVltVUFBQUFBQUFBQUFBPT0AuFFBQUFoQUlBRjBwaGEyVWdUMGhOSUMwZ1YyVnNZMjl0WlNCSWIyMWxBQk5CSUZnZ1V5QlVJRWdnV0NCVUlFa2dReUF1QUFBQUFBQUNKc2dBQ3pOUFowNVFUbTB3WjNoVkFBRUFLMmgwZEhCek9pOHZkM2QzTG5sdmRYUjFZbVV1WTI5dEwzZGhkR05vUDNZOU0wOW5UbEJPYlRCbmVGVUFCM2x2ZFhSMVltVUFBQUFBQUFBQUFBPT0A0FFBQUFtQUlBSzBkU1JVVk9JRTlTV0U1SFJTQjRJRk5sYm1RZ01TQXRJRk11V0M1T0xrUXVJRTR1V0M1RUxrVXVVeTRBRTBFZ1dDQlRJRlFnU0NCWUlGUWdTU0JESUM0QUFBQUFBQUhCT0FBTFVIRTRaakJTZG5OeGFGVUFBUUFyYUhSMGNITTZMeTkzZDNjdWVXOTFkSFZpWlM1amIyMHZkMkYwWTJnL2RqMVFjVGhtTUZKMmMzRm9WUUFIZVc5MWRIVmlaUUFBQUFBQUFBQUEAtFFBQUFnUUlBRjFSUFMxbFBUVUZPUlNBdElFUkZWa2xNSUU1SlIwaFVBQkJNU1ZSSVZVRk9TVUZPSUZCSVQwNUxBQUFBQUFBQ0l1QUFDM1JTV2s1c1pHZGFUblJOQUFFQUsyaDBkSEJ6T2k4dmQzZDNMbmx2ZFhSMVltVXVZMjl0TDNkaGRHTm9QM1k5ZEZKYVRteGtaMXBPZEUwQUIzbHZkWFIxWW1VQUFBQUFBQUFBQUE9PQC4UUFBQWhBSUFGME5oWlhSdmNtbGhJQzBnVkdobElGaGhiaUJEY21WM0FCTkJJRmdnVXlCVUlFZ2dXQ0JVSUVrZ1F5QXVBQUFBQUFBQ01vQUFDMUU0UmpaclZXNTJSR1JKQUFFQUsyaDBkSEJ6T2k4dmQzZDNMbmx2ZFhSMVltVXVZMjl0TDNkaGRHTm9QM1k5VVRoR05tdFZiblpFWkVrQUIzbHZkWFIxWW1VQUFBQUFBQUFBQUE9PQC4UUFBQWhRSUFHRWxTU1VSWVUwTllUbFFnTFNCQlJsUllVa0pWVWs1WVVnQVRRU0JZSUZNZ1ZDQklJRmdnVkNCSklFTWdMZ0FBQUFBQUFkaW9BQXRhTFhsWmRUQnJaamR4Y3dBQkFDdG9kSFJ3Y3pvdkwzZDNkeTU1YjNWMGRXSmxMbU52YlM5M1lYUmphRDkyUFZvdGVWbDFNR3RtTjNGekFBZDViM1YwZFdKbEFBQUFBQUFBQUFBPQDEUUFBQWpnSUFJVU5QVGxOVlRVRk9SU0F0SUU1dmRHaHBibWNnVEdGemRITWdSbTl5WlhabGNnQVRRU0JZSUZNZ1ZDQklJRmdnVkNCSklFTWdMZ0FBQUFBQUFmKzRBQXRqUlRSRFNWaEhURTlSYndBQkFDdG9kSFJ3Y3pvdkwzZDNkeTU1YjNWMGRXSmxMbU52YlM5M1lYUmphRDkyUFdORk5FTkpXRWRNVDFGdkFBZDViM1YwZFdKbEFBQUFBQUFBQUFBPQDgUUFBQXBBSUFOMEpCUkZSU1NWQWdUVlZUU1VNZ2VDQkhVa1ZGVGlCUFVsaE9SMFVnTFNCRVQwNG5WQ0JVUlV4TUlFMUZJRmRJUVZRZ1ZFOGdSRThBRTBFZ1dDQlRJRlFnU0NCWUlGUWdTU0JESUM0QUFBQUFBQUYrMEFBTFgwMUhSRTVOWlZob1kyY0FBUUFyYUhSMGNITTZMeTkzZDNjdWVXOTFkSFZpWlM1amIyMHZkMkYwWTJnL2RqMWZUVWRFVGsxbFdHaGpad0FIZVc5MWRIVmlaUUFBQUFBQUFBQUEAsFFBQUFnQUlBRTBkUFQxTWdMU0JDWlhOMElFWnlhV1Z1WkhNQUUwRWdXQ0JUSUZRZ1NDQllJRlFnU1NCRElDNEFBQUFBQUFJK09BQUxTRjl5U1VwaWNEbFFWR3NBQVFBcmFIUjBjSE02THk5M2QzY3VlVzkxZEhWaVpTNWpiMjB2ZDJGMFkyZy9kajFJWDNKSlNtSndPVkJVYXdBSGVXOTFkSFZpWlFBQUFBQUFBQUFBALRRQUFBZ1FJQUZFTlBTMFZaV2lBdElFUkZRVVFnUTA5U1RrVlNBQk5CSUZnZ1V5QlVJRWdnV0NCVUlFa2dReUF1QUFBQUFBQUIyS2dBQ3pkeFUybDJjRzVaV0ZkbkFBRUFLMmgwZEhCek9pOHZkM2QzTG5sdmRYUjFZbVV1WTI5dEwzZGhkR05vUDNZOU4zRlRhWFp3YmxsWVYyY0FCM2x2ZFhSMVltVUFBQUFBQUFBQUFBPT0A0FFBQUFsd0lBS2t0aGFYUnZJRk5vYjIxaElDMGdVMk5oY25rZ1IyRnljbmtnS0VwcFpHRnViMloxSUZKbGJXbDRLUUFUUVNCWUlGTWdWQ0JJSUZnZ1ZDQkpJRU1nTGdBQUFBQUFBcXVZQUF0WFQyOHhkelJIVWxwMll3QUJBQ3RvZEhSd2N6b3ZMM2QzZHk1NWIzVjBkV0psTG1OdmJTOTNZWFJqYUQ5MlBWZFBiekYzTkVkU1duWmpBQWQ1YjNWMGRXSmxBQUFBQUFBQUFBQT0AvFFBQUFpQUlBRzBoWVZsTkJSMFVnZUNCTFUweFdJQzBnVFZsVFZFVlNTVTlWVXdBVFFTQllJRk1nVkNCSUlGZ2dWQ0JKSUVNZ0xnQUFBQUFBQWV3d0FBdHNaR05hVVVWbU1tSjZid0FCQUN0b2RIUndjem92TDNkM2R5NTViM1YwZFdKbExtTnZiUzkzWVhSamFEOTJQV3hrWTFwUlJXWXlZbnB2QUFkNWIzVjBkV0psQUFBQUFBQUFBQUE9ALhRQUFBaEFJQUZ6WlpUbFJJVFVGT1JTQXRJRVpWVGtzZ1UxVk5UVVZTQUJOQklGZ2dVeUJVSUVnZ1dDQlVJRWtnUXlBdUFBQUFBQUFDTHBnQUMzaDFSRjlXY1RZdGQyMUpBQUVBSzJoMGRIQnpPaTh2ZDNkM0xubHZkWFIxWW1VdVkyOXRMM2RoZEdOb1AzWTllSFZFWDFaeE5pMTNiVWtBQjNsdmRYUjFZbVVBQUFBQUFBQUFBQT09ALhRQUFBaGdJQUdUVXdPU0JUU1VOQlVrbFBJQzBnZEc5cmVXOGdaSEpwZG1VQUUwRWdXQ0JUSUZRZ1NDQllJRlFnU1NCRElDNEFBQUFBQUFLTVdBQUxVa0pTVEdSaFltaFpZbWNBQVFBcmFIUjBjSE02THk5M2QzY3VlVzkxZEhWaVpTNWpiMjB2ZDJGMFkyZy9kajFTUWxKTVpHRmlhRmxpWndBSGVXOTFkSFZpWlFBQUFBQUFBQUFBANBRQUFBbGdJQUtVWnlaV1JrYVdVZ1JISmxaR1FnTFNCQmFXNG5kQ0JPYnlBb1RVWWdUa0ZUVkZrZ1VtVnRhWGdwQUJOQklGZ2dVeUJVSUVnZ1dDQlVJRWtnUXlBdUFBQUFBQUFDUWlBQUMxaHlUMmRwVjNFeWVXZzBBQUVBSzJoMGRIQnpPaTh2ZDNkM0xubHZkWFIxWW1VdVkyOXRMM2RoZEdOb1AzWTlXSEpQWjJsWGNUSjVhRFFBQjNsdmRYUjFZbVVBQUFBQUFBQUFBQT09ALBRQUFBZ0FJQUZsUkZUbE5WSUMwZ1ZrOHhSQ0JYTHlCc01UbFZNVVFBRUZWT1JFVlNSMUpQVlU1RUlGcFBUa1VBQUFBQUFBTDlvQUFMV0ZoNWRTMVlSVXRYZGtrQUFRQXJhSFIwY0hNNkx5OTNkM2N1ZVc5MWRIVmlaUzVqYjIwdmQyRjBZMmcvZGoxWVdIbDFMVmhGUzFkMlNRQUhlVzkxZEhWaVpRQUFBQUFBQUFBQQCgUUFBQWNnSUFEa3hsWm5SdmVpQXRJRUZUVkZKUEFBcExiMjVrZW1sMVJHVjJBQUFBQUFBQ1BqZ0FDMEZaWm5KU1pYVnljMjFCQUFFQUsyaDBkSEJ6T2k4dmQzZDNMbmx2ZFhSMVltVXVZMjl0TDNkaGRHTm9QM1k5UVZsbWNsSmxkWEp6YlVFQUIzbHZkWFIxWW1VQUFBQUFBQUFBQUE9PQDAUUFBQWlnSUFIVWxTVlV0QklIZ2dKRmRGVWxaRklDMGdUazlYSUVrblRTQklTVWRJQUJOQklGZ2dVeUJVSUVnZ1dDQlVJRWtnUXlBdUFBQUFBQUFDYVRBQUMxTmxhblZOVW14NldFRkpBQUVBSzJoMGRIQnpPaTh2ZDNkM0xubHZkWFIxWW1VdVkyOXRMM2RoZEdOb1AzWTlVMlZxZFUxU2JIcFlRVWtBQjNsdmRYUjFZbVVBQUFBQUFBQUFBQT09AMRRQUFBalFJQUlGWk1RVVJKVFVsU0lETXpNaUF0SUU1RlZrVlNJRWRKVmtVZ1FTQkdLa05MQUJOQklGZ2dVeUJVSUVnZ1dDQlVJRWtnUXlBdUFBQUFBQUFCLzdnQUN6SnBkVkV5ZHpWaVUyTjNBQUVBSzJoMGRIQnpPaTh2ZDNkM0xubHZkWFIxWW1VdVkyOXRMM2RoZEdOb1AzWTlNbWwxVVRKM05XSlRZM2NBQjNsdmRYUjFZbVVBQUFBQUFBQUFBQT09AKhRQUFBZWdJQURVeGxablJ2ZWlBdElGcFlRMElBRTBFZ1dDQlRJRlFnU0NCWUlGUWdTU0JESUM0QUFBQUFBQUtJY0FBTGJXSXlXbEprVTJGeWNUZ0FBUUFyYUhSMGNITTZMeTkzZDNjdWVXOTFkSFZpWlM1amIyMHZkMkYwWTJnL2RqMXRZakphVW1SVFlYSnhPQUFIZVc5MWRIVmlaUUFBQUFBQUFBQUEAsFFBQUFnQUlBRTBOc2FYaHBaSFZ6SUMwZ1QzWmxjbk5sWVhNQUUwRWdXQ0JUSUZRZ1NDQllJRlFnU1NCRElDNEFBQUFBQUFJWEtBQUxWRU5IWVdSUlpHSjFZbk1BQVFBcmFIUjBjSE02THk5M2QzY3VlVzkxZEhWaVpTNWpiMjB2ZDJGMFkyZy9kajFVUTBkaFpGRmtZblZpY3dBSGVXOTFkSFZpWlFBQUFBQUFBQUFBALhRQUFBaEFJQUdsWkJURWtrUWtWQlZGTWdMU0JCVGtkRlRDQW1JRVJGVFU5T0FCQk1TVlJJVlVGT1NVRk9JRkJJVDA1TEFBQUFBQUFDL2FBQUMybGxVMFZJUm1VMlNFeFpBQUVBSzJoMGRIQnpPaTh2ZDNkM0xubHZkWFIxWW1VdVkyOXRMM2RoZEdOb1AzWTlhV1ZUUlVoR1pUWklURmtBQjNsdmRYUjFZbVVBQUFBQUFBQUFBQT09AMhRQUFBa2dJQUpVUmxZV1J6YjNWc0lDWWdWazlNVkNCV1NWTkpUMDRnTFNCRVlYa2dUMllnUkhKbFlXMEFFMEVnV0NCVElGUWdTQ0JZSUZRZ1NTQkRJQzRBQUFBQUFBSGdlQUFMVjNWMFRHSlBRbVJLTmtVQUFRQXJhSFIwY0hNNkx5OTNkM2N1ZVc5MWRIVmlaUzVqYjIwdmQyRjBZMmcvZGoxWGRYUk1ZazlDWkVvMlJRQUhlVzkxZEhWaVpRQUFBQUFBQUFBQQC0UUFBQWdRSUFGSE52ZG1semN5QXRJR3RwZEhSNUlIQm9iMjVyQUJOQklGZ2dVeUJVSUVnZ1dDQlVJRWtnUXlBdUFBQUFBQUFCWTNnQUMweGFaMVpCZGtWcFlXUXdBQUVBSzJoMGRIQnpPaTh2ZDNkM0xubHZkWFIxWW1VdVkyOXRMM2RoZEdOb1AzWTlURnBuVmtGMlJXbGhaREFBQjNsdmRYUjFZbVVBQUFBQUFBQUFBQT09AMxRQUFBbEFJQUoxQklRVkpOUVVOSlUxUWdMU0JOU1V4TFdTQlhRVmtnZHk4Z1RGVkhRU0FtSURZZ1UwVk9XZ0FUUVNCWUlGTWdWQ0JJSUZnZ1ZDQkpJRU1nTGdBQUFBQUFBZkFZQUFzNVZrUmthWHBoYkhKMFJRQUJBQ3RvZEhSd2N6b3ZMM2QzZHk1NWIzVjBkV0psTG1OdmJTOTNZWFJqYUQ5MlBUbFdSR1JwZW1Gc2NuUkZBQWQ1YjNWMGRXSmxBQUFBQUFBQUFBQT0AsFFBQUFnQUlBRTAxWVVsUkJUQ0F0SUVaRlFWSWdSa1ZCVTFRQUUwRWdXQ0JUSUZRZ1NDQllJRlFnU1NCRElDNEFBQUFBQUFKVnFBQUxkbkpDVUhkWFUwSlpZWGNBQVFBcmFIUjBjSE02THk5M2QzY3VlVzkxZEhWaVpTNWpiMjB2ZDJGMFkyZy9kajEyY2tKUWQxZFRRbGxoZHdBSGVXOTFkSFZpWlFBQUFBQUFBQUFBALxRQUFBaHdJQUdrcHZhRzV1ZVNCTGFXUWdMU0JEUVU0blZDQlRWRTlRSUZWVEFCTkJJRmdnVXlCVUlFZ2dXQ0JVSUVrZ1F5QXVBQUFBQUFBQ1pVZ0FDMHhhY1VGUVZ6ZzJZekZKQUFFQUsyaDBkSEJ6T2k4dmQzZDNMbmx2ZFhSMVltVXVZMjl0TDNkaGRHTm9QM1k5VEZweFFWQlhPRFpqTVVrQUIzbHZkWFIxWW1VQUFBQUFBQUFBQUE9PQCUUUFBQWF3SUFDVkJCVWtGRVNVZE5RUUFJVFVNZ1QxSlRSVTRBQUFBQUFBSWUrQUFMVm5WbVMwNTZaM0YyTm1NQUFRQXJhSFIwY0hNNkx5OTNkM2N1ZVc5MWRIVmlaUzVqYjIwdmQyRjBZMmcvZGoxV2RXWkxUbnBuY1hZMll3QUhlVzkxZEhWaVpRQUFBQUFBQUFBQQDIUUFBQWtBSUFKMDFKVTFSQklGQk1RVmxCSUMwZ1MybHNiR0VnVTJocGRDQmJJRkJvYjI1cklGZGxaV3NnWFFBUFNYWmhiaWR6SUZCc1lYbHNhWE4wQUFBQUFBQUNCNGdBQzFOUVgwdGlXbFJCUzJSbkFBRUFLMmgwZEhCek9pOHZkM2QzTG5sdmRYUjFZbVV1WTI5dEwzZGhkR05vUDNZOVUxQmZTMkphVkVGTFpHY0FCM2x2ZFhSMVltVUFBQUFBQUFBQUFBPT0A0FFBQUFtQUlBTGxSQldpMHhPRGNnTFNCRlRrUk1SVk5USUV4UFZrVWdWeThnVjFKWVEwdE5RVTVGSUNZZ1FWQllRMGhGT1RRQUVFeEpWRWhWUVU1SlFVNGdVRWhQVGtzQUFBQUFBQU5qTUFBTE56UjJVVzAyVUV0SlN6Z0FBUUFyYUhSMGNITTZMeTkzZDNjdWVXOTFkSFZpWlM1amIyMHZkMkYwWTJnL2RqMDNOSFpSYlRaUVMwbExPQUFIZVc5MWRIVmlaUUFBQUFBQUFBQUEAtFFBQUFnUUlBSFVKeVpYUjBJRTFoZG1WeWFXTnJJQzBnUkdseWRIa2dSR2x1Ym1WeUFBcFZTMEpoYzNOc2FXNWxBQUFBQUFBRHFZQUFDMDFvYUVoYU9HMUxTbUpGQUFFQUsyaDBkSEJ6T2k4dmQzZDNMbmx2ZFhSMVltVXVZMjl0TDNkaGRHTm9QM1k5VFdob1NGbzRiVXRLWWtVQUIzbHZkWFIxWW1VQUFBQUFBQUFBQUE9PQ=="

    @JvmStatic
    fun main(args: Array<String>) {
//        val future = CompletableFuture<AudioPlaylist>()
//
//        manager.loadItem("https://www.youtube.com/playlist?list=PLBJg8xCfBYcpJr_hicjzjwVw7gxV4H7bY", object : AudioLoadResultHandler {
//            override fun trackLoaded(track: AudioTrack) {
//                println("loaded track for playlist?")
//            }
//
//            override fun playlistLoaded(playlist: AudioPlaylist) {
//                future.complete(playlist)
//            }
//
//            override fun loadFailed(exception: FriendlyException) {
//                exception.printStackTrace()
//            }
//
//            override fun noMatches() {
//                println("no matches")
//            }
//        })
//
//        future.thenApply(::encode).thenAccept(::println).get()

        decodePlaylist(playlist).let {
            println(it.name)
            println(it.tracks.size)
        }
    }

    private fun encode(playlist: AudioPlaylist): String {
        val baos = ByteArrayOutputStream()
        val output = MessageOutput(baos)
        val stream = output.startMessage()

        val encodedTracks = playlist.tracks.map(PlaylistSerialization::encode)

        stream.writeUTF(playlist.name)
        stream.writeInt(encodedTracks.size)

        for (track in encodedTracks) {
            stream.writeUTF(track)
        }

        output.commitMessage()
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    private fun encode(track: AudioTrack): String {
        val baos = ByteArrayOutputStream()
        val output = MessageOutput(baos)
        manager.encodeTrack(output, track)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    private fun decodePlaylist(encoded: String): AudioPlaylist {
        val bytes = Base64.getDecoder().decode(encoded)
        val bais = ByteArrayInputStream(bytes)
        val input = MessageInput(bais)
        val stream = input.nextMessage()

        val name = stream.readUTF()
        val trackCount = stream.readInt()
        val tracks = mutableListOf<AudioTrack>()

        for (i in 0 until trackCount) {
            tracks.add(decodeTrack(stream.readUTF()))
        }

        return BasicAudioPlaylist(name, tracks, null, false)
    }

    private fun decodeTrack(encoded: String): AudioTrack {
        val bytes = Base64.getDecoder().decode(encoded)
        val baos = ByteArrayInputStream(bytes)
        return manager.decodeTrack(MessageInput(baos)).decodedTrack
    }
}